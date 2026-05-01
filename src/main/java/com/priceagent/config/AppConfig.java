package com.priceagent.config;

import com.priceagent.agent.PriceAgent;
import com.priceagent.db.DatabaseInitializer;
import com.priceagent.db.DatabaseManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

@Configuration
public class AppConfig {

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.model:openai/gpt-4o-mini}")
    private String openAiModel;

    @Value("${openai.base.url:https://openrouter.ai/api/v1}")
    private String openAiBaseUrl;

    @Value("${db.path:price-agent.db}")
    private String dbPath;

    @Bean
    public DatabaseManager databaseManager() {
        return new DatabaseManager("jdbc:sqlite:" + dbPath);
    }

    @Bean
    public DatabaseInitializer databaseInitializer(DatabaseManager databaseManager) throws SQLException {
        DatabaseInitializer initializer = new DatabaseInitializer(databaseManager);
        initializer.initialize();
        return initializer;
    }

    @Bean
    public PriceAgent priceAgent(DatabaseManager databaseManager) {
        return new PriceAgent(openAiApiKey, openAiModel, openAiBaseUrl, databaseManager);
    }
}
