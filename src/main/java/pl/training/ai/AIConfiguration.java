package pl.training.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import pl.training.ai.chat.ChatController;
import pl.training.ai.chat.PowerTool;

import java.util.function.Function;

@Configuration
public class AIConfiguration {

    @Bean
    public ChatClient openaiChatClient (OpenAiChatModel openAiChatModel) {
        return ChatClient.create(openAiChatModel);
    }

    @Bean
    public ChatClient ollamaChatClient (OllamaChatModel ollamaChatModel) {
        return ChatClient.create(ollamaChatModel);
    }

    /*@Bean
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel) {
        return ChatClient.create(chatModel);
    }*/

    @Description("Calculates power of two")
    @Bean
    public Function<ChatController.ValueOfDouble, Double> power() {
        return new PowerTool();
    }

}
