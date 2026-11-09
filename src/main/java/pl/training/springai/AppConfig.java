package pl.training.springai;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.http.HttpRequest;

import static java.net.http.HttpRequest.newBuilder;

@Configuration
public class AppConfig {

    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel chatModel, ChatMemory chatMemory) {
        // return ChatClient.create(chatModel);
        return ChatClient.builder(chatModel)
                // .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // .defaultTools()
                // .defaultOptions()
                .build();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository)
                .build();
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(DataSource dataSource) {
        // return new InMemoryChatMemoryRepository();
        return JdbcChatMemoryRepository.builder()
                .dataSource(dataSource)
                .dialect(JdbcChatMemoryRepositoryDialect.from(dataSource))
                .build();
    }

    @Bean
    public McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpClientCustomizer() {
        return (name, builder) -> {
            if (name.equals("mcp-search")) {
                builder.requestBuilder(newBuilder()
                        .header("Authorization", "Bearer " + System.getenv("MCP_TOKEN")))
                ;
            }
        };
    }

}
