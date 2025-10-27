package pl.training.ai.chat;

import pl.training.ai.chat.ChatController.ValueOfDouble;

import java.util.function.Function;

public class PowerTool implements Function<ValueOfDouble, Double> {

    @Override
    public Double apply(ValueOfDouble value) {
        return value.value() * value.value();
    }

}
