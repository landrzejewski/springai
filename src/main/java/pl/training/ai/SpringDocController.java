package pl.training.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
public class SpringDocController {

    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;

    public SpringDocController(OllamaChatModel chatModel, PgVectorStore vectorStore,
                               @Value("classpath:prompts/detailed-response.st") Resource promptResource) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
        this.promptTemplate = new PromptTemplate(promptResource);
    }

    @PostMapping(value = "spring-doc-text")
    public Flux<String> trainings(@RequestBody PromptRequest promptRequest) {
        Map<String, Object> parameters = Map.of("userQuestion", promptRequest.message());
        var enhancedPrompt = promptTemplate.create(parameters)
                .getContents();
        return chatClient
                .prompt(enhancedPrompt)
                .stream()
                .content();
    }

    @PostMapping(value = "spring-doc")
    public String trainingsAsText(@RequestBody PromptRequest promptRequest) {
        Map<String, Object> parameters = Map.of("userQuestion", promptRequest.message());
        var enhancedPrompt = promptTemplate.create(parameters)
                .getContents();
        return chatClient
                .prompt(enhancedPrompt)
                .call()
                .content();
    }

}
