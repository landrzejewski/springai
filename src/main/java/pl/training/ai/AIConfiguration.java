package pl.training.ai;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import pl.training.ai.chat.ChatController;
import pl.training.ai.chat.PowerTool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Function;

@Configuration
public class AIConfiguration {

    @Bean
    public ChatClient openaiChatClient (OpenAiChatModel openAiChatModel) {
        return ChatClient.create(openAiChatModel);
    }

    @Bean
    public ChatClient ollamaChatClient (OllamaChatModel ollamaChatModel) {
        return ChatClient.create(ollamaChatModel);
    }

    /*@Bean
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel) {
        return ChatClient.create(chatModel);
    }*/

    @Description("Calculates power of two")
    @Bean
    public Function<ChatController.ValueOfDouble, Double> power() {
        return new PowerTool();
    }

    @Value("classpath:trainings.json")
    private Resource trainings;

    @Value("classpath:vector-store.json")
    private Resource vectorStore;

    @Bean
    public SimpleVectorStore localVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) throws IOException {
        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        if (vectorStore.exists()) {
            System.out.println("Loading vector store");
            simpleVectorStore.load(vectorStore);
        } else {
            System.out.println("Creating vector store");
            var textReader = new TextReader(trainings);
            textReader.getCustomMetadata().put("filename", "trainings.json");
            var documents = textReader.get();
            var textSplitter = new TokenTextSplitter();
            var splitDocuments = textSplitter.split(documents);
            simpleVectorStore.add(splitDocuments);
            var path = Paths.get("src", "main", "resources");
            var file = path.toFile().getAbsolutePath() + "/vector-store.json";
            simpleVectorStore.save(new File(file).getAbsoluteFile());
        }
        return simpleVectorStore;
    }

    @Autowired
    private JdbcClient jdbcClient;

    @Value("classpath:spring-framework.pdf")
    private Resource springDoc;

    @Autowired
    private PgVectorStore externalVectorStore;

    /*@PostConstruct
    public void initVectorStore() {
        var count = jdbcClient.sql("select count(*) from vector_store")
                .query(Integer.class)
                .single();
        if (count == 0) {
            System.out.println("Creating pgvector store");
            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopPagesToSkipBeforeDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();
            var pdfReader = new PagePdfDocumentReader(springDoc, config);
            var textSplitter = new TokenTextSplitter();
            var documents = textSplitter.apply(pdfReader.get());
            externalVectorStore.accept(documents);
        }
    }*/


}
