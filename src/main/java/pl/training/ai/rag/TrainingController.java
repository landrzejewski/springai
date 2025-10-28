package pl.training.ai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TrainingController {

    private final ChatClient chatClient;

    public TrainingController(OllamaChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
             //   .defaultAdvisors(new QuestionA)
                .build();
    }

}
