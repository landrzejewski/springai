package pl.training.springai.tools;

import java.util.function.Function;

public class PowerTool implements Function<Double, Double> {

    @Override
    public Double apply(Double value) {
        return value * value;
    }

}
