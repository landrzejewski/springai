package pl.training.springai.controllers;

import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
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
public class AudioController {

    private final TextToSpeechModel textToSpeechModel;
    private final TranscriptionModel transcriptionModel;

    public AudioController(TextToSpeechModel textToSpeechModel, TranscriptionModel transcriptionModel) {
        this.textToSpeechModel = textToSpeechModel;
        this.transcriptionModel = transcriptionModel;
    }

    @PostMapping("text-to-speech")
    public ResponseEntity<byte[]> textToSpeech(@RequestBody PromptRequest promptRequest) {
        var options = TextToSpeechOptions.builder()
                //.model("tts-1-hd")
                .model("tts-qwen3")
                //.voice("alloy")
                .voice("l-understated-2")
                .format("mp3")
                .speed(1.0)
                .build();
        var prompt = new TextToSpeechPrompt(promptRequest.userPromptText(), options);
        var bytes = textToSpeechModel.call(prompt)
                .getResult()
                .getOutput();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeq")
                .header(FileUploadBase.CONTENT_DISPOSITION, "attachment; filename=\"audio.mp3\"")
                .body(bytes);
    }

    @Value("classpath:audio.mp3")
    private Resource audio;

    @PostMapping("audio-transcription")
    public String audioTranscription() {
        var prompt = new AudioTranscriptionPrompt(audio);
        return transcriptionModel.call(prompt)
                .getResult()
                .getOutput();
    }

}
