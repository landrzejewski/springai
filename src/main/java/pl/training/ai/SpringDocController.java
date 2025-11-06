package pl.training.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class SpringDocController {

    private final ChatClient chatClient;

    public SpringDocController(OllamaChatModel chatModel, PgVectorStore vectorStore) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    @PostMapping("spring-doc")
    public Flux<String> trainings(@RequestBody PromptRequest promptRequest) {
        return chatClient
                .prompt(promptRequest.message())
                .stream()
                .content();
    }

}
