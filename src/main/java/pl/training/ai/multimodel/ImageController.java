package pl.training.ai.multimodel;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ChatModelCallAdvisor;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.ai.chat.PromptRequest;

import java.util.Map;

@RestController
public class ImageController {

    private final OpenAiImageModel openAiImageModel;
    private final ChatClient chatClient;

    @Value("classpath:image.png")
    private Resource image;

    public ImageController(OpenAiImageModel openAiImageModel, @Qualifier("openaiChatClient") ChatClient chatClient) {
        this.openAiImageModel = openAiImageModel;
        this.chatClient = chatClient;
    }

    @GetMapping("generate-image")
    public String generateImage(@RequestBody PromptRequest promptRequest) {
        var options = OpenAiImageOptions.builder()
                .model("dall-e-3")
                .width(1024)
                .height(1024)
                .quality("hd")
                .style("natural")
                .build();
        var imagePrompt = new ImagePrompt(promptRequest.message(), options);
        return openAiImageModel.call(imagePrompt)
                .getResult()
                .getOutput()
                // .getB64Json()
                .getUrl();
    }

    @GetMapping("generate-description")
    public String generateDescription() {
        return chatClient
                .prompt()
                .user(spec -> spec
                        .text("Can you explain what you see in the following image")
                        .media(MediaType.IMAGE_PNG, image)
                )
                .call()
                .content();
    }

}
