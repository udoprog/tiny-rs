package eu.toolchain.rs.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;

import lombok.Data;

@Data
public class ChainStatement {
    private static final Joiner PARAMETER_JOINER = Joiner.on(", ");

    private final List<String> format = new ArrayList<>();
    private final List<Object> arguments = new ArrayList<>();

    public ChainStatement add(final String format, Object... arguments) {
        this.format.add(format);
        this.arguments.addAll(Arrays.asList(arguments));
        return this;
    }

    public ChainStatement addVarString(final String format,
            final List<String> parameters) {
        if (parameters.isEmpty()) {
            return this;
        }

        this.format.add(String.format(format,
                PARAMETER_JOINER.join(parameters.stream().map(s -> "$S").iterator())));
        this.arguments.addAll(parameters);
        return this;
    }

    public String format() {
        return String.join("", format);
    }

    public Object[] arguments() {
        return arguments.toArray();
    }
}
