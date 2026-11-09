package pl.training.springai.advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

public class TimestampAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        var response = callAdvisorChain.nextCall(chatClientRequest);
        var text = response.chatResponse().getResult().getOutput().getText();
        var modifiedText = text + "\n Timestamp: " + LocalDateTime.now();
        var assistantMessage = new AssistantMessage(modifiedText);
        var newResponse = ChatResponse.builder()
                .generations(List.of(new Generation(assistantMessage)))
                .build();
        return response.mutate().chatResponse(newResponse).build();
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return streamAdvisorChain.nextStream(chatClientRequest)
                .concatWith(Flux.defer(() -> Flux.just(responseWithTimestamp())));
    }

    private ChatClientResponse responseWithTimestamp() {
        var modifiedText = "\nTimestamp: " + LocalDateTime.now();
        var assistantMessage = new AssistantMessage(modifiedText);
        return ChatClientResponse.builder()
                .chatResponse(ChatResponse.builder()
                        .generations(List.of(new Generation(assistantMessage)))
                        .build())
                .build();
    }

    @Override
    public String getName() {
        return TimestampAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
