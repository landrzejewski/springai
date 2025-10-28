package pl.training.ai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.ai.chat.PromptRequest;

@RestController
public class TrainingController {

    private final ChatClient chatClient;

    public TrainingController(OpenAiChatModel chatModel, VectorStore vectorStore) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    @PostMapping("trainings")
    public TrainingList trainings(@RequestBody PromptRequest promptRequest) {
        return chatClient
                .prompt()
                .user(promptRequest.message())
                .call()
                .entity(TrainingList.class);
    }

}
