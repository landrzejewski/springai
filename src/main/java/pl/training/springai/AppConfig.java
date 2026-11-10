package pl.training.springai;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;

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

    @Value("classpath:books-catalog.json")
    private Resource booksCatalog;

    public void initPgVector(PgVectorStore vectorStore) {
        var documents = createDocuments();
        vectorStore.add(documents);
    }

    @Primary
    @Bean
    public EmbeddingModel embeddingModel(OpenAiEmbeddingModel embeddingModel) {
        return embeddingModel;
    }

    @Bean
    public SimpleVectorStore simpleVectorStore(OpenAiEmbeddingModel embeddingModel) {
        var vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        var documents = createDocuments();
        vectorStore.add(documents);
        return vectorStore;
    }

    private List<Document> createDocuments() {
        var jsonReader = new JsonReader(booksCatalog, "title", "author", "genre", "year", "description");
        List<Document> rawDocuments = jsonReader.get();
        return rawDocuments.stream()
                .map(doc -> {
                    String content = doc.getText();
                    var metadata = new HashMap<String, Object>();
                    for (String line : content.split("\n")) {
                        if (line.startsWith("title: ")) {
                            metadata.put("title", line.substring(7));
                        } else if (line.startsWith("author: ")) {
                            metadata.put("author", line.substring(8));
                        } else if (line.startsWith("genre: ")) {
                            metadata.put("genre", line.substring(7));
                        } else if (line.startsWith("year: ")) {
                            try {
                                metadata.put("year", Integer.parseInt(line.substring(6)));
                            } catch (NumberFormatException e) {
                                metadata.put("year", line.substring(6));
                            }
                        }
                    }

                    return new Document(doc.getId(), content, metadata);
                })
                .toList();
    }

}
