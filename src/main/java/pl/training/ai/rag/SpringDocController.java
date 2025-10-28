package pl.training.ai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.ai.chat.PromptRequest;

import java.util.HashMap;
import java.util.List;

@RestController
public class SpringDocController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:prompts/spring-doc.st")
    private Resource springDocPrompt;

    public SpringDocController(OpenAiChatModel chatModel, PgVectorStore vectorStore) {
        this.chatClient = ChatClient.builder(chatModel)
                //.defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
        this.vectorStore = vectorStore;
    }

    @PostMapping("trainings")
    public String trainings(@RequestBody PromptRequest promptRequest) {
        var promptTemplate = new PromptTemplate(springDocPrompt);
        var params = new HashMap<String, Object>();
        params.put("input", promptRequest.message());
        params.put("documents", String.join("\n", findSimilar(promptRequest.message())));
        var prompt = promptTemplate.create(params);

        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }

    private List<String> findSimilar(String message) {
        var documents = vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(5).build());
        return documents.stream().map(Document::getFormattedContent).toList();
    }

}
