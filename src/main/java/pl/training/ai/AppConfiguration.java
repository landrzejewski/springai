package pl.training.ai;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
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

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class AppConfiguration {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PgVectorStore externalVectorStore;

    @Value("classpath:spring-data-jpa-reference.pdf")
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
        if (count == 0) { // Fixed: should be == 0, not != 0
            System.out.println("Creating pgvector store");

            // 1. Enhanced PDF configuration
            var config = PdfDocumentReaderConfig.builder()
                    .withPagesPerDocument(1)
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
//                            .withNumberOfBottomTextLinesToDelete(3)
//                            .withNumberOfTopPagesToSkipBeforeDelete(1)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            var splitDocuments = getDocuments(config);

            externalVectorStore.accept(splitDocuments);

            System.out.println("Indexed " + splitDocuments.size() + " document chunks");
        }
    }

    private List<Document> getDocuments(PdfDocumentReaderConfig config) {
        var pdfReader = new PagePdfDocumentReader(springDoc, config);

        // 2. Better text splitting configuration
        var textSplitter = new TokenTextSplitter(
                500,  // defaultChunkSize - optimal for most embeddings
                100,  // minChunkSizeChars
                50,   // minChunkLengthToEmbed
                2,    // maxNumChunks
                true  // keepSeparator
        );

        // 3. Add metadata enrichment
        var documents = pdfReader.get();
        documents.forEach(doc -> {
            doc.getMetadata().put("source", "spring_documentation");
            doc.getMetadata().put("indexed_date", LocalDateTime.now().toString());
        });

        // 4. Apply splitting with better context
        return textSplitter.apply(documents);
    }

}
