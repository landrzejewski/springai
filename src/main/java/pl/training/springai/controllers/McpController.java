package pl.training.springai.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.springai.model.PromptRequest;
import pl.training.springai.tools.DateTimeTool;
import reactor.core.publisher.Flux;

import java.util.Arrays;

@RestController
public class McpController {

    private final ChatClient chatClient;
    private final ObjectProvider<ToolCallbackProvider> toolCallbackProvider;

    public McpController(ChatClient chatClient, ObjectProvider<ToolCallbackProvider> toolCallbackProvider) {
        this.chatClient = chatClient;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @PostMapping("mcp-client")
    public Flux<String> mcpClient(@RequestBody PromptRequest  promptRequest) {
        toolCallbackProvider.stream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .map(ToolCallback::getToolDefinition)
                .map(definition -> definition.name() + " - " + definition.description())
                .forEach(System.out::println);

        var providers = toolCallbackProvider.stream().toList();

        return chatClient.prompt()
                .user(promptRequest.userPromptText())
                .tools(providers)
                .stream()
                .content();
    }

}
