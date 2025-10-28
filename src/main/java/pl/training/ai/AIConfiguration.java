package pl.training.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.io.Resource;
import pl.training.ai.chat.ChatController;
import pl.training.ai.chat.PowerTool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Function;

@Configuration
public class AIConfiguration {

    @Bean
    public ChatClient openaiChatClient (OpenAiChatModel openAiChatModel) {
        return ChatClient.create(openAiChatModel);
    }

    @Bean
    public ChatClient ollamaChatClient (OllamaChatModel ollamaChatModel) {
        return ChatClient.create(ollamaChatModel);
    }

    /*@Bean
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel) {
        return ChatClient.create(chatModel);
    }*/

    @Description("Calculates power of two")
    @Bean
    public Function<ChatController.ValueOfDouble, Double> power() {
        return new PowerTool();
    }

    @Value("classpath:trainings.json")
    private Resource trainings;

    @Value("classpath:vector-store.json")
    private Resource vectorStore;

    @Bean
    public SimpleVectorStore vectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) throws IOException {
        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        if (vectorStore.exists()) {
            System.out.println("Loading vector store");
            simpleVectorStore.load(vectorStore);
        } else {
            System.out.println("Creating vector store");
            var textReader = new TextReader(trainings);
            textReader.getCustomMetadata().put("filename", "trainings.json");
            var documents = textReader.get();
            var textSplitter = new TokenTextSplitter();
            var splitDocuments = textSplitter.split(documents);
            simpleVectorStore.add(splitDocuments);
            var path = Paths.get("src", "main", "resources");
            var file = path.toFile().getAbsolutePath() + "/vector-store.json";
            simpleVectorStore.save(new File(file).getAbsoluteFile());
        }
        return simpleVectorStore;
    }

}
