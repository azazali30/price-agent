package com.priceagent.agent;

import com.priceagent.db.DatabaseManager;
import com.priceagent.tools.ExecuteSqlTool;
import com.priceagent.tools.GetDimensionValuesTool;
import com.priceagent.tools.SearchProductsTool;
import com.agentic4j.core.StreamingResponse;
import com.agentic4j.core.agent.AgentBuilder;
import com.agentic4j.core.annotation.SystemPrompt;
import com.agentic4j.core.memory.SlidingWindowMemory;
import com.agentic4j.openai.OpenAiChatModel;
import com.agentic4j.openai.OpenAiStreamingChatModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PriceAgent {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final DatabaseManager dbManager;
    private final Map<String, PriceAssistant> sessions = new ConcurrentHashMap<String, PriceAssistant>();
    private final Map<String, StreamingPriceAssistant> streamingSessions = new ConcurrentHashMap<String, StreamingPriceAssistant>();

    public interface PriceAssistant {
        @SystemPrompt(fromResource = "system-prompt.txt")
        String chat(String userMessage);
    }

    public interface StreamingPriceAssistant {
        @SystemPrompt(fromResource = "system-prompt.txt")
        StreamingResponse chat(String userMessage);
    }

    public PriceAgent(String apiKey, String model, String baseUrl, DatabaseManager dbManager) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.dbManager = dbManager;
    }

    public String chat(String sessionId, String userMessage) {
        PriceAssistant assistant = sessions.get(sessionId);
        if (assistant == null) {
            assistant = createAssistant(sessionId);
            sessions.put(sessionId, assistant);
        }
        return assistant.chat(userMessage);
    }

    public StreamingResponse chatStream(String sessionId, String userMessage) {
        StreamingPriceAssistant assistant = streamingSessions.get(sessionId);
        if (assistant == null) {
            assistant = createStreamingAssistant(sessionId);
            streamingSessions.put(sessionId, assistant);
        }
        return assistant.chat(userMessage);
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        streamingSessions.remove(sessionId);
    }

    private PriceAssistant createAssistant(String sessionId) {
        OpenAiChatModel.Builder builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.0);

        if (baseUrl != null && baseUrl.trim().length() > 0) {
            builder.baseUrl(baseUrl);
        }

        OpenAiChatModel chatModel = builder.build();

        return AgentBuilder.forInterface(PriceAssistant.class)
                .chatModel(chatModel)
                .memory(new SlidingWindowMemory(20))
                .tools(new ExecuteSqlTool(dbManager), new GetDimensionValuesTool(dbManager), new SearchProductsTool(dbManager))
                .build();
    }

    private StreamingPriceAssistant createStreamingAssistant(String sessionId) {
        OpenAiStreamingChatModel.Builder builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.0);

        if (baseUrl != null && baseUrl.trim().length() > 0) {
            builder.baseUrl(baseUrl);
        }

        OpenAiStreamingChatModel streamingModel = builder.build();

        return AgentBuilder.forInterface(StreamingPriceAssistant.class)
                .streamingChatModel(streamingModel)
                .memory(new SlidingWindowMemory(20))
                .tools(new ExecuteSqlTool(dbManager), new GetDimensionValuesTool(dbManager), new SearchProductsTool(dbManager))
                .build();
    }
}
