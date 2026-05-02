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
  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

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

    try {
      const response = await fetch(`${API_URL}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userMessage, sessionId })
      })
      const data = await response.json()
      setSessionId(data.sessionId)
      setMessages(prev => [...prev, { role: 'assistant', content: data.response }])
    } catch (error) {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Error connecting to server. Make sure backend is running.'
      }])
    } finally {
      setIsLoading(false)
    }
  }

  const resetChat = async () => {
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
                {msg.role === 'assistant' ? '🤖' : '👤'}
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

          {isLoading && (
            <div className="message assistant">
              <div className="message-avatar">{'🤖'}</div>
              <div className="message-content thinking-bubble">
                <span className="thinking-text">Thinking...</span>
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
          <button
            type="submit"
            disabled={!input.trim() || isLoading}
            className="send-btn"
          >
            Send
          </button>
        </form>
      </footer>
    </div>
  )
}

export default App
