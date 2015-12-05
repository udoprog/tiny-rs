package eu.toolchain.rs.processor;

import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AnnotationValues {
    private final Element element;
    private final AnnotationMirror annotation;
    private final Map<String, AnnotationValue> values;

    public Optional<AnnotationField> get(final String key) {
        return ofNullable(values.get(key))
                .map(value -> new AnnotationField(element, annotation, value));
    }
}
