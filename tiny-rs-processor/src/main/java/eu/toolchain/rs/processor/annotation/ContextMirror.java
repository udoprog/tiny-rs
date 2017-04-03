package eu.toolchain.rs.processor.annotation;

import eu.toolchain.rs.processor.AnnotationValues;
import eu.toolchain.rs.processor.RsUtils;
import eu.toolchain.rs.processor.result.Result;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import lombok.Data;

@Data
public class ContextMirror {
    private final AnnotationMirror annotation;

    public static Result<ContextMirror> getFor(final RsUtils utils, final Element element,
            final AnnotationMirror a) {
        final AnnotationValues values = utils.getElementValuesWithDefaults(element, a);
        return Result.ok(new ContextMirror(a));
    }
}
