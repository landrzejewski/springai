package pl.training.ai.multimodel;

import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.training.ai.chat.PromptRequest;

@RestController
public class AudioController {

    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

    public AudioController(OpenAiAudioSpeechModel openAiAudioSpeechModel) {
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
    }

    @GetMapping("generate-audio")
    public ResponseEntity<byte[]> generateAudio(@RequestBody PromptRequest promptRequest) {
        var options = OpenAiAudioSpeechOptions.builder()
                .model("tts-1-hd")
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .speed(1.0f)
                .build();

        var speechPrompt = new SpeechPrompt(promptRequest.message(), options);
        var bytes = openAiAudioSpeechModel.call(speechPrompt)
                .getResult()
                .getOutput();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audio.mp3\"")
                .body(bytes);

    }

}
