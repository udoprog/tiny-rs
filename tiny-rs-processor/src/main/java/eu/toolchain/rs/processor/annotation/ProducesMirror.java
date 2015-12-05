package eu.toolchain.rs.processor.annotation;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import eu.toolchain.rs.processor.AnnotationField;
import eu.toolchain.rs.processor.AnnotationValues;
import eu.toolchain.rs.processor.RsUtils;
import eu.toolchain.rs.processor.result.Result;
import lombok.Data;

@Data
public class ProducesMirror {
    private final AnnotationMirror annotation;
    private final List<String> value;

    public static Result<ProducesMirror> getFor(final RsUtils utils, final Element element,
            final AnnotationMirror a) {
        final AnnotationValues values = utils.getElementValuesWithDefaults(element, a);

        final List<String> value = values.get("value").map(AnnotationField::asStringArray)
                .orElseThrow(() -> new IllegalArgumentException("value"));

        return Result.ok(new ProducesMirror(a, value));
    }
}
