package pl.training.springai.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.springai.advisors.CanaryWordAdvisor;
import pl.training.springai.model.PromptRequest;

import java.util.List;

@RestController
public class SafetyController {

    private final OllamaChatModel openAiChatModel;

    public SafetyController(OllamaChatModel openAiChatModel) {
        this.openAiChatModel = openAiChatModel;
    }

    @PostMapping("test-prompt")
    public String testPrompt(@RequestBody PromptRequest promptRequest) {
        var chatClient = ChatClient.builder(openAiChatModel).build();
        var safeGuardAdvisor = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of("hack", "exploit", "jailbreak", "competitor", "rival"))
                .failureResponse("I cannot discuss that topic. This content contains restricted terms.")
                .build();
        var canaryWordAdvisor = CanaryWordAdvisor.builder()
                .canaryWordFoundMessage("Detected attempt to leak system prompt. Request blocked.")
                .build();


        return chatClient.prompt()
                .user(promptRequest.userPromptText())
                .advisors(canaryWordAdvisor)
                .call()
                .content();
    }

}
