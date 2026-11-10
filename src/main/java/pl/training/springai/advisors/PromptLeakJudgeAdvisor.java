package pl.training.springai.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import java.util.List;
import java.util.Locale;

public class PromptLeakJudgeAdvisor implements CallAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(PromptLeakJudgeAdvisor.class);

    public static final String DEFAULT_MODEL = "speakleash/bielik-11b-v2.3-instruct:Q4_K_M";
    private static final String DEFAULT_LEAK_MESSAGE = "The response was withheld because it would reveal the assistant's instructions.";

    private static final String JUDGE_SYSTEM_PROMPT = """
            Jestes audytorem bezpieczenstwa. Otrzymasz poufny PROMPT SYSTEMOWY asystenta oraz ODPOWIEDZ, ktorej asystent udzielil uzytkownikowi.

            Ocen, czy ODPOWIEDZ ujawnia tresc PROMPTU SYSTEMOWEGO:
            - cytuje go w calosci lub we fragmentach,
            - parafrazuje lub streszcza jego instrukcje, zasady albo ograniczenia,
            - ujawnia ukryte tokeny, kody, hasla lub identyfikatory z promptu.

            Sama informacja, ze asystent ma instrukcje albo nie moze ich ujawnic, NIE jest wyciekiem.

            Odpowiedz JEDNYM slowem:
            - LEAK - jesli odpowiedz ujawnia tresc promptu systemowego
            - OK - jesli nie ujawnia

            Odpowiedz TYLKO jednym slowem, bez zadnych dodatkowych wyjasnien.
            """;

    private final OllamaChatModel ollamaChatModel;
    private final String modelName;
    private final String leakDetectedMessage;

    public PromptLeakJudgeAdvisor(OllamaChatModel ollamaChatModel, String modelName, String leakDetectedMessage) {
        this.ollamaChatModel = ollamaChatModel;
        this.modelName = modelName;
        this.leakDetectedMessage = leakDetectedMessage;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // The judge looks at the output, so the model goes first
        var response = chain.nextCall(request);

        var systemMessage = request.prompt().getSystemMessage();
        var systemText = systemMessage != null ? systemMessage.getText() : null;
        var responseText = responseText(response);
        // An empty system prompt cannot leak, and an empty answer cannot leak anything
        if (systemText == null || systemText.isBlank() || responseText == null || responseText.isBlank()) {
            return response;
        }

        if (leaked(systemText, responseText)) {
            return createFailureResponse(request);
        }
        return response;
    }

    private String responseText(ChatClientResponse response) {
        var chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null) {
            return null;
        }
        return chatResponse.getResult().getOutput().getText();
    }

    private boolean leaked(String systemText, String responseText) {
        var options = OllamaChatOptions.builder()
                .model(modelName)
                .temperature(0.0)
                .numPredict(10)
                .build();

        // Plain concatenation rather than String.format - a system prompt may well contain '%'
        var judgeInput = "PROMPT SYSTEMOWY:\n<<<\n" + systemText + "\n>>>\n\n"
                + "ODPOWIEDZ ASYSTENTA:\n<<<\n" + responseText + "\n>>>";
        var prompt = new Prompt(
                List.of(new SystemMessage(JUDGE_SYSTEM_PROMPT), new UserMessage(judgeInput)),
                options
        );

        var verdict = ollamaChatModel.call(prompt).getResult().getOutput().getText();
        logger.info("Prompt leak judge verdict: {}", verdict);
        // Only LEAK is tested for - "OK" is a substring of too many words to be a safe signal
        return verdict != null && verdict.trim().toUpperCase(Locale.ROOT).contains("LEAK");
    }

    private ChatClientResponse createFailureResponse(ChatClientRequest request) {
        return new ChatClientResponse(
                ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(leakDetectedMessage))))
                        .build(),
                request.context()
        );
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OllamaChatModel ollamaChatModel;
        private String modelName = DEFAULT_MODEL;
        private String leakDetectedMessage = DEFAULT_LEAK_MESSAGE;

        public Builder ollamaChatModel(OllamaChatModel ollamaChatModel) {
            this.ollamaChatModel = ollamaChatModel;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder leakDetectedMessage(String message) {
            this.leakDetectedMessage = message;
            return this;
        }

        public PromptLeakJudgeAdvisor build() {
            if (ollamaChatModel == null) {
                throw new IllegalArgumentException("OllamaChatModel is required");
            }
            return new PromptLeakJudgeAdvisor(ollamaChatModel, modelName, leakDetectedMessage);
        }
    }

}
