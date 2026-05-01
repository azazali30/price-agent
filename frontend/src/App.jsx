import { useState, useRef, useEffect } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import './App.css'

const API_URL = import.meta.env.VITE_API_URL || '/api'

function App() {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: 'Hello! I\'m your Market Price Assistant. I can help you browse products, compare prices across stores, find the cheapest options, and analyze price trends across UAE emirates.\n\nTry asking me things like:\n- "What dairy products do you have?"\n- "Find the cheapest milk in Dubai"\n- "Compare yogurt prices across stores"\n- "Show me price trends for rice"'
    }
  ])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [sessionId, setSessionId] = useState(null)
  const [streamingContent, setStreamingContent] = useState('')
  const [toolStatus, setToolStatus] = useState('')
  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)
  const abortControllerRef = useRef(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamingContent, toolStatus])

  useEffect(() => {
    inputRef.current?.focus()
  }, [isLoading])

  const sendMessage = async (e) => {
    e.preventDefault()
    if (!input.trim() || isLoading) return

    const userMessage = input.trim()
    setInput('')
    setMessages(prev => [...prev, { role: 'user', content: userMessage }])
    setIsLoading(true)
    setStreamingContent('')
    setToolStatus('')

    abortControllerRef.current = new AbortController()

    try {
      const response = await fetch(`${API_URL}/chat/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userMessage, sessionId }),
        signal: abortControllerRef.current.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let accumulated = ''
      let currentSessionId = sessionId

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // SSE events are separated by double newline
        const events = buffer.split('\n\n')
        buffer = events.pop() || ''

        for (const event of events) {
          if (!event.trim()) continue
          const lines = event.split('\n')
          let type = ''
          let dataLines = []

          for (const line of lines) {
            if (line.startsWith('event:')) {
              type = line.slice(6).trim()
            } else if (line.startsWith('data:')) {
              dataLines.push(line.slice(5))
            }
          }

          // SSE spec: multiple data lines are joined with newline
          const data = dataLines.join('\n')

          if (!type || !data) continue
          console.log('[SSE]', type, JSON.stringify(data.substring(0, 80)))

          if (type === 'session') {
            currentSessionId = data
            setSessionId(data)
          } else if (type === 'tool') {
            setToolStatus(`Running: ${data}`)
          } else if (type === 'token') {
            setToolStatus('')
            accumulated += data
            setStreamingContent(accumulated)
          } else if (type === 'done') {
            setToolStatus('')
            // Use server's authoritative response text, fall back to accumulated tokens
            const finalText = data.trim() ? data : accumulated
            if (finalText.trim()) {
              setMessages(prev => [...prev, { role: 'assistant', content: finalText }])
            }
            setStreamingContent('')
            accumulated = ''
          } else if (type === 'error') {
            setToolStatus('')
            setMessages(prev => [...prev, { role: 'assistant', content: `Error: ${data}` }])
            setStreamingContent('')
            accumulated = ''
          }
        }
      }

      // If stream ended without a done event, finalize whatever we have
      if (accumulated.trim()) {
        setMessages(prev => [...prev, { role: 'assistant', content: accumulated }])
        setStreamingContent('')
      } else {
        setStreamingContent('')
      }

    } catch (error) {
      if (error.name === 'AbortError') {
        if (streamingContent) {
          setMessages(prev => [...prev, { role: 'assistant', content: streamingContent }])
          setStreamingContent('')
        }
      } else {
        // Fallback to non-streaming endpoint
        try {
          const response = await fetch(`${API_URL}/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: userMessage, sessionId })
          })
          const data = await response.json()
          setSessionId(data.sessionId)
          setMessages(prev => [...prev, { role: 'assistant', content: data.response }])
        } catch (fallbackError) {
          setMessages(prev => [...prev, {
            role: 'assistant',
            content: `Error connecting to server. Make sure backend is running.`
          }])
        }
      }
    } finally {
      setIsLoading(false)
      abortControllerRef.current = null
    }
  }

  const stopGeneration = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
    }
  }

  const resetChat = async () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
    }
    if (sessionId) {
      try {
        await fetch(`${API_URL}/chat/reset`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sessionId })
        })
      } catch (e) { /* ignore */ }
    }
    setSessionId(null)
    setStreamingContent('')
    setMessages([{
      role: 'assistant',
      content: 'Chat reset. How can I help you with market prices today?'
    }])
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-content">
          <h1>Market Price Agent</h1>
          <p className="subtitle">UAE Product Price Monitoring</p>
        </div>
        <button className="reset-btn" onClick={resetChat} title="New conversation">
          New Chat
        </button>
      </header>

      <main className="chat-container">
        <div className="messages">
          {messages.map((msg, idx) => (
            <div key={idx} className={`message ${msg.role}`}>
              <div className="message-avatar">
                {msg.role === 'assistant' ? '\uD83E\uDD16' : '\uD83D\uDC64'}
              </div>
              <div className="message-content">
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    code({ node, className, children, ...props }) {
                      const isBlock = className || (typeof children === 'string' && children.includes('\n'))
                      if (isBlock) {
                        return (
                          <pre className="code-block">
                            <code className={className} {...props}>{children}</code>
                          </pre>
                        )
                      }
                      return <code className="inline-code" {...props}>{children}</code>
                    },
                    pre({ children }) {
                      return <>{children}</>
                    },
                    table({ children }) {
                      return <div className="table-wrapper"><table>{children}</table></div>
                    }
                  }}
                >
                  {msg.content}
                </ReactMarkdown>
              </div>
            </div>
          ))}

          {/* Streaming message */}
          {streamingContent && (
            <div className="message assistant">
              <div className="message-avatar">{'\uD83E\uDD16'}</div>
              <div className="message-content">
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    code({ node, className, children, ...props }) {
                      const isBlock = className || (typeof children === 'string' && children.includes('\n'))
                      if (isBlock) {
                        return (
                          <pre className="code-block">
                            <code className={className} {...props}>{children}</code>
                          </pre>
                        )
                      }
                      return <code className="inline-code" {...props}>{children}</code>
                    },
                    pre({ children }) {
                      return <>{children}</>
                    },
                    table({ children }) {
                      return <div className="table-wrapper"><table>{children}</table></div>
                    }
                  }}
                >
                  {streamingContent}
                </ReactMarkdown>
                <span className="cursor-blink">|</span>
              </div>
            </div>
          )}

          {/* Loading indicator (when waiting for first token / tool execution) */}
          {isLoading && !streamingContent && (
            <div className="message assistant">
              <div className="message-avatar">{'\uD83E\uDD16'}</div>
              <div className="message-content thinking-bubble">
                <span className="thinking-text">{toolStatus || 'Thinking...'}</span>
                <span className="dot-pulse"></span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>
      </main>

      <footer className="input-area">
        <form onSubmit={sendMessage} className="input-form">
          <input
            ref={inputRef}
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about products, prices, stores..."
            disabled={isLoading}
            className="chat-input"
          />
          {isLoading ? (
            <button
              type="button"
              onClick={stopGeneration}
              className="stop-btn"
            >
              Stop
            </button>
          ) : (
            <button
              type="submit"
              disabled={!input.trim()}
              className="send-btn"
            >
              Send
            </button>
          )}
        </form>
      </footer>
    </div>
  )
}

export default App
