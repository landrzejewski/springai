package pl.training.ai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final Map<String, ChatClient> chatClients;

    /*public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }*/

    /*public ChatController(@Qualifier("openaiChatClient") ChatClient chatClient,
                          Map<String, ChatClient> chatClients) {
        this.chatClient = chatClient;
        this.chatClients = chatClients;
    }*/

    public ChatController(OpenAiChatModel chatModel,
                          Map<String, ChatClient> chatClients,
                          ChatMemory chatMemory) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.chatClients = chatClients;
    }

    @PostMapping("chat")
    public String chat(@RequestBody PromptRequest promptRequest) {
        return chatClient.prompt()
                .user(promptRequest.message())
                .call()
                .content();  // short for getResult().getOutput().getContent();
    }

    @PostMapping("jokes")
    public String jokes(@RequestParam(defaultValue = "software developer") String topic) {
        return chatClient.prompt()
                .user(spec -> spec
                        .text("Tell me a joke about {topic}")
                        .param("topic", topic)
                )
                .call()
                .content();
    }

    @PostMapping("tourist-attractions")
    public Flux<String> touristAttractions() {
        return chatClient.prompt()
                .user("I am visiting Poland can you give me 10 places I must visit")
                .stream()
                .content()
                .map(text -> text.toLowerCase(Locale.ROOT));
    }

    @Value("classpath:prompts/dev-assistant.st")
    private Resource devAssistantSystemMessage;

    @PostMapping("dev-assistant")
    public Flux<String> devAssistant(
            @RequestBody PromptRequest promptRequest,
            @RequestParam(defaultValue = "Java") String programmingLanguage) {
       /*var devAssistantSystemMessage = """
                You are a helpful assistant who answers questions related to {programming_language}.
                For any other questions, respond with “I don’t know!”
                """;*/
        var chatOptions = ChatOptions.builder()
                .temperature(0.1)
                .maxTokens(1_000)
                .build();

        Map<String, Object> params = Map.of("programming_language", programmingLanguage);
        var userMessage = new UserMessage(promptRequest.message());
        var systemMessageTemplate = new SystemPromptTemplate(devAssistantSystemMessage);
        var systemMessage = systemMessageTemplate.createMessage(params);
        var prompt = new Prompt(List.of(systemMessage, userMessage));

        return chatClient
                .prompt(prompt)
                .options(chatOptions)
                /*.user(promptRequest.message())
                .system(spec -> spec
                        .text(devAssistantSystemMessage)
                        .params(params)
                )*/
                .stream()
                .content();
    }

    private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());

    @PostMapping("conversation")
    public String conversation(@RequestBody PromptRequest promptRequest) {
        /*var prompts = new Prompt(List.of(
                new UserMessage("Hi my name is Luc"),
                new AssistantMessage("Nice to meet you!"),
                new UserMessage(promptRequest.message())
        ));*/
        var summary = getMessagesSummary();
        messages.add(new UserMessage(promptRequest.message()));
        System.out.println("###################");
        System.out.println(summary);
        System.out.println("###################");
        var response = chatClient
                //.prompt(new Prompt(messages))
                .prompt()
                .system(summary)
                .user(promptRequest.message())
                .call()
                .content();
        if (response != null) {
            messages.add(new AssistantMessage(response));
        }
        return response;
    }

    private String getMessagesSummary() {
        var text = messages.stream()
                .map(Message::getText)
                .collect(Collectors.joining());
        System.out.println("Text: " + text);
        if (text.isBlank()) {
            return "-";
        }
        return chatClients.get("ollamaChatClient")
                .prompt()
                .user(spec -> spec
                        .text("""
                                Summarize only the information explicitly stated in the source text, without adding 
                                any external details or interpretations. Present the most important facts in no more than 10 concise 
                                sentences. Source text: {text}""")
                        .param("text", text)
                )
                .call()
                .content();
    }

    @PostMapping("stateful-conversation")
    public String statefulConversation(@RequestBody PromptRequest promptRequest) {
       return chatClient
                .prompt()
                .advisors(
                        SimpleLoggerAdvisor.builder().build(),
                        new TimestampAdvisor()
                )
                .user(promptRequest.message())
                .call()
                .content();
    }

    // Few shot example
    @PostMapping("structured-by-prompt")
    public String structuredByPrompt(@RequestBody PromptRequest promptRequest) {
        return chatClient
                .prompt()
                .user(promptRequest.message())
                .call()
                .content();
    }

    @PostMapping("structured-by-type")
    public Book structuredByType(@RequestBody PromptRequest promptRequest) {
        return chatClient
                .prompt()
                .user(promptRequest.message())
                .call()
                .entity(Book.class);
    }

    @PostMapping("structured-by-parametrized-type")
    public List<Book> structuredByParametrizedType(@RequestBody PromptRequest promptRequest) {
        return chatClient
                .prompt()
                .user(promptRequest.message())
                .call()
                .entity(new ParameterizedTypeReference<List<Book>>() {});
    }

    // Map can be created using parametrized typ or using converter
    @PostMapping("structured-as-map")
    public Object structuredAsMap(@RequestBody PromptRequest promptRequest) {
        var mapOutputConverter = new MapOutputConverter();
        var format = mapOutputConverter.getFormat();
        System.out.println("format: " + format);
        String template = """
        Input :  {input}
        {format}
        """;
        var promptTemplate = new PromptTemplate(template);
        var message = promptTemplate.createMessage(Map.of("input", promptRequest.message(), "format", format));
        var promptMessage = new Prompt(List.of(message));
        var result = chatClient
                .prompt(promptMessage)
                .call()
                .content();
        return mapOutputConverter.convert(result);
    }

}
