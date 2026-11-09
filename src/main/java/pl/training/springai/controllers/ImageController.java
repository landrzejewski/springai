package pl.training.springai.controllers;

import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.springai.model.PromptRequest;
import reactor.core.publisher.Flux;

import java.util.Base64;

@RestController
public class ImageController {

    private final ImageModel imageModel;
    private final ChatClient chatClient;

    public ImageController(ImageModel imageModel, ChatClient chatClient) {
        this.imageModel = imageModel;
        this.chatClient = chatClient;
    }

    @PostMapping("image-generation")
    public ResponseEntity<byte[]> imageGeneration(@RequestBody PromptRequest promptRequest) {
        var options = ImageOptionsBuilder.builder()
                //.model("gpt-image-2.5-flare")
                .model("qwen-image-hq")
                .width(1024)
                .height(768)
                //.responseFormat("url")
                .build();
        var prompt = new ImagePrompt(promptRequest.userPromptText(), options);
        var image = imageModel.call(prompt)
                .getResult()
                .getOutput()
                .getB64Json();
        var bytes = Base64.getDecoder().decode(image);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/png")
                .header(FileUploadBase.CONTENT_DISPOSITION, "attachment; filename=\"image.png\"")
                .body(bytes);
    }

    @Value("classpath:image.jpg")
    private Resource image;

    @PostMapping("image-description")
    public Flux<String> imageDescription(@RequestBody PromptRequest promptRequest) {
        return chatClient.prompt()
                .user(spec -> spec
                        .text(promptRequest.userPromptText())
                        .media(MediaType.IMAGE_JPEG, image)
                )
                .stream()
                .content();
    }


}
