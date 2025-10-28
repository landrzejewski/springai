package pl.training.ai.multimodel;

import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.ai.chat.PromptRequest;

import java.util.Map;

@RestController
public class ImageController {

    private final OpenAiImageModel openAiImageModel;

    public ImageController(OpenAiImageModel openAiImageModel) {
        this.openAiImageModel = openAiImageModel;
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

}
