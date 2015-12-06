package eu.toolchain.rs.processor.annotation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import eu.toolchain.rs.processor.AnnotationValues;
import eu.toolchain.rs.processor.RsUtils;
import eu.toolchain.rs.processor.result.Result;
import lombok.Data;

@Data
public class SuspendedMirror {
    private final AnnotationMirror annotation;

    public static Result<SuspendedMirror> getFor(final RsUtils utils, final Element element,
            final AnnotationMirror a) {
        final AnnotationValues values = utils.getElementValuesWithDefaults(element, a);
        return Result.ok(new SuspendedMirror(a));
    }
}