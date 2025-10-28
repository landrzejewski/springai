package pl.training.ai.mcp;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.ai.chat.PromptRequest;
import reactor.core.publisher.Flux;

@RestController
public class McpController {

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    public McpController(@Qualifier("openaiChatClient") ChatClient chatClient, ToolCallbackProvider tools) {
        this.chatClient = chatClient;
        this.toolCallbackProvider = tools;
    }

    @PostMapping("search")
    public String search(@RequestBody PromptRequest promptRequest) {
        return chatClient.prompt()
                .toolCallbacks(toolCallbackProvider)
                .user(promptRequest.message())
                .call()
                .content();
    }
}
