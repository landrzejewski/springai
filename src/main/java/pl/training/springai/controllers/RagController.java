package pl.training.springai.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.training.springai.AppConfig;
import pl.training.springai.model.PromptRequest;
import reactor.core.publisher.Flux;

@RestController
public class RagController {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;

    private final ChatClient chatClient;
    private final SimpleVectorStore simpleVectorStore;
    private final AppConfig appConfig;
    private final PgVectorStore pgVectorStore;

    public RagController(ChatClient chatClient, SimpleVectorStore simpleVectorStore, AppConfig appConfig, PgVectorStore pgVectorStore) {
        this.chatClient = chatClient;
        this.simpleVectorStore = simpleVectorStore;
        this.appConfig = appConfig;
        this.pgVectorStore = pgVectorStore;
    }

    @PostMapping("init-pgvector")
    public void initPgVector() {
        appConfig.initPgVector(pgVectorStore);
    }

    private VectorStore getVectorStore(String name) {
        return "simple".equals(name) ? simpleVectorStore : pgVectorStore;
    }

    private VectorStoreDocumentRetriever getVectorStoreDocumentRetriever(VectorStore vectorStore, int topK, double threshold) {
        return VectorStoreDocumentRetriever.builder()
                .topK(topK)
                .similarityThreshold(threshold)
                .vectorStore(vectorStore)
                .build();
    }

    @PostMapping("basic-rag-query")
    public Flux<String> basicRagQuery(
            @RequestBody PromptRequest promptRequest,
            @RequestParam(defaultValue = "simple") String storeType) {
        var store = getVectorStore(storeType);
        var retriever = getVectorStoreDocumentRetriever(store, DEFAULT_TOP_K,  DEFAULT_SIMILARITY_THRESHOLD);
        var retrieverAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .build();
        return chatClient.prompt()
                .advisors(retrieverAdvisor)
                .user(promptRequest.userPromptText())
                .stream()
                .content();
    }


}
