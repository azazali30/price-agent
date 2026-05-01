package com.priceagent.controller;

import com.priceagent.agent.PriceAgent;
import dev.langchain4j.service.TokenStream;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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
        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        try {
            String response = priceAgent.chat(sessionId, request.message());
            return ResponseEntity.ok(Map.of(
                "response", response,
                "sessionId", sessionId
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "response", "Error: " + e.getMessage(),
                "sessionId", sessionId
            ));
        }
    }

    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout
        final String finalSessionId = sessionId;

        try {
            // Send session ID first
            emitter.send(SseEmitter.event()
                    .name("session")
                    .data(finalSessionId, MediaType.TEXT_PLAIN));

            TokenStream tokenStream = priceAgent.chatStream(finalSessionId, request.message());

            tokenStream
                    .onToolExecuted(toolExecution -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("tool")
                                    .data(toolExecution.request().name(), MediaType.TEXT_PLAIN));
                        } catch (IOException e) {
                            // ignore
                        }
                    })
                    .onNext(token -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(token, MediaType.TEXT_PLAIN));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .onComplete(response -> {
                        try {
                            // Send the complete response text so frontend has authoritative final content
                            String fullText = response.content().text() != null ? response.content().text() : "";
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(fullText, MediaType.TEXT_PLAIN));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .onError(error -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(error.getMessage() != null ? error.getMessage() : "Unknown error", MediaType.TEXT_PLAIN));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
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
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    public record ChatRequest(String message, String sessionId) {}
}
