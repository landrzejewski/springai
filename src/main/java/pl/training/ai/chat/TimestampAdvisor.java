package pl.training.ai.chat;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.time.Instant;
import java.util.List;

public class TimestampAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return TimestampAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        var response = callAdvisorChain.nextCall(chatClientRequest);
        var originalContent = response.chatResponse().getResult().getOutput().getText();
        var modifiedContent = originalContent + "\n" + "Response timestamp: " + Instant.now();
        var assistantMessage = new AssistantMessage(modifiedContent);
        var newResponse = ChatResponse.builder()
                .generations(List.of(new Generation(assistantMessage)))
                .build();
        return response.mutate().chatResponse(newResponse).build();
    }

}
