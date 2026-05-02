package com.priceagent.controller;

import com.priceagent.agent.PriceAgent;
import com.agentic4j.core.ChatResponse;
import com.agentic4j.core.StreamingResponse;
import com.agentic4j.core.ToolExecutionResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final PriceAgent priceAgent;

    public ChatController(PriceAgent priceAgent) {
        this.priceAgent = priceAgent;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        try {
            String response = priceAgent.chat(sessionId, request.getMessage());
            Map<String, String> body = new HashMap<String, String>();
            body.put("response", response);
            body.put("sessionId", sessionId);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, String> body = new HashMap<String, String>();
            body.put("response", "Error: " + e.getMessage());
            body.put("sessionId", sessionId);
            return ResponseEntity.status(500).body(body);
        }
    }

    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        final SseEmitter emitter = new SseEmitter(120_000L);
        final String finalSessionId = sessionId;

        try {
            emitter.send(SseEmitter.event()
                    .name("session")
                    .data(finalSessionId, MediaType.TEXT_PLAIN));

            StreamingResponse streamingResponse = priceAgent.chatStream(finalSessionId, request.getMessage());

            streamingResponse
                    .onToolExecuted(new Consumer<ToolExecutionResult>() {
                        @Override
                        public void accept(ToolExecutionResult result) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("tool")
                                        .data(result.getToolName(), MediaType.TEXT_PLAIN));
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    })
                    .onToken(new Consumer<String>() {
                        @Override
                        public void accept(String token) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(token, MediaType.TEXT_PLAIN));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                    })
                    .onComplete(new Consumer<ChatResponse>() {
                        @Override
                        public void accept(ChatResponse response) {
                            try {
                                String fullText = "";
                                if (response.getMessage() != null && response.getMessage().getContent() != null) {
                                    fullText = response.getMessage().getContent();
                                }
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(fullText, MediaType.TEXT_PLAIN));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                    })
                    .onError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable error) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(error.getMessage() != null ? error.getMessage() : "Unknown error", MediaType.TEXT_PLAIN));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                    })
                    .start();

        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(e.getMessage()));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }

        return emitter;
    }

    @PostMapping("/chat/reset")
    public ResponseEntity<Map<String, String>> resetSession(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId != null) {
            priceAgent.clearSession(sessionId);
        }
        Map<String, String> body = new HashMap<String, String>();
        body.put("status", "ok");
        return ResponseEntity.ok(body);
    }

    public static class ChatRequest {
        private String message;
        private String sessionId;

        public ChatRequest() {}

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}
