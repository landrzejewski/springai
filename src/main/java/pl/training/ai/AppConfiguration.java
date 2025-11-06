package pl.training.ai;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
public class AppConfiguration {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PgVectorStore externalVectorStore;

    @Value("classpath:spring-framework.pdf")
    private Resource springDoc;

    @Bean
    public ChatClient ollamaChatClient (OllamaChatModel ollamaChatModel) {
        return ChatClient.create(ollamaChatModel);
    }

    @PostConstruct
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
    }

}
