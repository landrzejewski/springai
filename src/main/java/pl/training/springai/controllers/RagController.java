package pl.training.springai.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.training.springai.AppConfig;
import pl.training.springai.model.PromptRequest;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class RagController {

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.5;

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final SimpleVectorStore simpleVectorStore;
    private final AppConfig appConfig;
    private final PgVectorStore pgVectorStore;

    public RagController(ChatClient chatClient, @Qualifier("openAiChatModel") ChatModel chatModel, SimpleVectorStore simpleVectorStore, AppConfig appConfig, PgVectorStore pgVectorStore) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
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

    @PostMapping("rag-query-expander")
    public Flux<String> ragQueryExpander(
            @RequestBody PromptRequest promptRequest,
            @RequestParam(defaultValue = "simple") String storeType) {
        var store = getVectorStore(storeType);
        var queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();
        var retriever = getVectorStoreDocumentRetriever(store, DEFAULT_TOP_K,  DEFAULT_SIMILARITY_THRESHOLD);
        var retrieverAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryExpander(queryExpander)
                .build();
        return chatClient.prompt()
                .advisors(retrieverAdvisor, new SimpleLoggerAdvisor())
                .user(promptRequest.userPromptText())
                .stream()
                .content();
    }

    @PostMapping("rag-rewrite-query")
    public Flux<String> ragRewriteQuery(
            @RequestBody PromptRequest promptRequest,
            @RequestParam(defaultValue = "simple") String storeType) {
        var store = getVectorStore(storeType);
        var rewriteTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
        var translationTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .targetLanguage("English")
                .build();
        var retriever = getVectorStoreDocumentRetriever(store, DEFAULT_TOP_K,  DEFAULT_SIMILARITY_THRESHOLD);
        var retrieverAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryTransformers(rewriteTransformer, translationTransformer)
                .build();
        return chatClient.prompt()
                .advisors(retrieverAdvisor, new SimpleLoggerAdvisor())
                .user(promptRequest.userPromptText())
                .stream()
                .content();
    }

    @PostMapping("rag-filter-by-metadata")
    public Flux<String> ragFilterByMetadata(@RequestBody PromptRequest promptRequest,
                                            @RequestParam(required = false) String genre,
                                            @RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) String author
    ) {
        var filterExpression = buildFilterExpression(genre, year, author);
        var searchRequestBuilder = SearchRequest.builder()
                .query(promptRequest.userPromptText())
                .topK(DEFAULT_TOP_K)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD);
        if (filterExpression != null) {
            var expression = new FilterExpressionTextParser().parse(filterExpression);
            searchRequestBuilder.filterExpression(expression);
        }
        var documents = pgVectorStore.similaritySearch(searchRequestBuilder.build());
        // documents.get(0).getMetadata();
        var context = documents.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining());

        // Hand-written augmentation - this is what ContextualQueryAugmenter does for you
        var systemMessage = """
              Answer the question using only the context below.
              If the context is empty or does not contain the answer, say that you do not know.

              CONTEXT:
              %s
               """.formatted(context);

        return ChatClient.builder(chatModel).build()
                .prompt()
                .user(promptRequest.userPromptText())
                .system(systemMessage)
                .stream()
                .content();
    }

    // Builds the portable filter syntax understood by FilterExpressionTextParser
    private String buildFilterExpression(String genre, Integer year, String author) {
        var expr = new StringBuilder();

        if (genre != null) {
            expr.append(String.format("genre == '%s'", genre));
        }
        if (year != null) {
            if (!expr.isEmpty()) expr.append(" && ");
            expr.append(String.format("year >= %d", year));
        }
        if (author != null) {
            if (!expr.isEmpty()) expr.append(" && ");
            expr.append(String.format("author == '%s'", author));
        }

        return expr.isEmpty() ? null : expr.toString();
    }


    @PostMapping("rag-debug")
    public void debug(@RequestBody PromptRequest promptRequest) {
        var searchRequest = SearchRequest.builder()
                .query(promptRequest.userPromptText())
                .topK(DEFAULT_TOP_K)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .build();
        var documents = pgVectorStore.similaritySearch(searchRequest);
        documents.forEach(System.out::println);
    }


}
