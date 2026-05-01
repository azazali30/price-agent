package com.priceagent.agent;

import com.priceagent.db.DatabaseManager;
import com.priceagent.tools.ExecuteSqlTool;
import com.priceagent.tools.GetDimensionValuesTool;
import com.priceagent.tools.SearchProductsTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PriceAgent {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final DatabaseManager dbManager;
    private final Map<String, PriceAssistant> sessions = new ConcurrentHashMap<>();
    private final Map<String, StreamingPriceAssistant> streamingSessions = new ConcurrentHashMap<>();

    public interface PriceAssistant {
        @SystemMessage(fromResource = "system-prompt.txt")
        String chat(String userMessage);
    }

    public interface StreamingPriceAssistant {
        @SystemMessage(fromResource = "system-prompt.txt")
        TokenStream chat(String userMessage);
    }

    public PriceAgent(String apiKey, String model, String baseUrl, DatabaseManager dbManager) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.dbManager = dbManager;
    }

    public String chat(String sessionId, String userMessage) {
        PriceAssistant assistant = sessions.computeIfAbsent(sessionId, this::createAssistant);
        return assistant.chat(userMessage);
    }

    public TokenStream chatStream(String sessionId, String userMessage) {
        StreamingPriceAssistant assistant = streamingSessions.computeIfAbsent(sessionId, this::createStreamingAssistant);
        return assistant.chat(userMessage);
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        streamingSessions.remove(sessionId);
    }

    private PriceAssistant createAssistant(String sessionId) {
        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.0)
                .logRequests(false)
                .logResponses(false);

        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        OpenAiChatModel chatModel = builder.build();

        return AiServices.builder(PriceAssistant.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(new ExecuteSqlTool(dbManager), new GetDimensionValuesTool(dbManager), new SearchProductsTool(dbManager))
                .build();
    }

    private StreamingPriceAssistant createStreamingAssistant(String sessionId) {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.0)
                .logRequests(false)
                .logResponses(false);

        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        OpenAiStreamingChatModel streamingModel = builder.build();

        return AiServices.builder(StreamingPriceAssistant.class)
                .streamingChatLanguageModel(streamingModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(new ExecuteSqlTool(dbManager), new GetDimensionValuesTool(dbManager), new SearchProductsTool(dbManager))
                .build();
    }
}
