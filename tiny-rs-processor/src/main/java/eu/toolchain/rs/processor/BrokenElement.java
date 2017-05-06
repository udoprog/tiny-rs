package eu.toolchain.rs.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import lombok.Data;

@Data
public class BrokenElement extends RuntimeException {
    private final Element element;

    public BrokenElement(
            final String message, final Element element
    ) {
        super(message);
        this.element = element;
    }

    public void writeTo(final Messager messager) {
        messager.printMessage(Diagnostic.Kind.ERROR, getMessage(), element);
    }
}
