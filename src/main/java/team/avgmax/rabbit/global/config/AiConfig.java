package team.avgmax.rabbit.global.config;

import java.io.IOException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model}")
    private String openAiModel;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double openAiTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens}")
    private Integer openAiMaxTokens;    

    @Bean
    OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl("https://api.openai.com")
                .apiKey(openAiApiKey)
                .build();
    }

    @Bean
    OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(openAiModel)
                        .temperature(openAiTemperature)
                        .maxTokens(openAiMaxTokens)
                        .build())
                .build();
    }

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().build();
    }

    @Bean
    ChatClient chatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) throws IOException {
        
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
