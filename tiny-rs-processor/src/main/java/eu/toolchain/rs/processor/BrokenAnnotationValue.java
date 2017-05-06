package eu.toolchain.rs.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import lombok.Data;

@Data
public class BrokenAnnotationValue extends BrokenElement {
    private final AnnotationMirror annotation;
    private final AnnotationValue value;

    public BrokenAnnotationValue(
            final String message, final Element element, final AnnotationMirror annotation,
            final AnnotationValue value
    ) {
        super(message, element);
        this.annotation = annotation;
        this.value = value;
    }

    @Override
    public void writeTo(final Messager messager) {
        messager.printMessage(Diagnostic.Kind.ERROR, getMessage(), getElement(), annotation, value);
    }
}
