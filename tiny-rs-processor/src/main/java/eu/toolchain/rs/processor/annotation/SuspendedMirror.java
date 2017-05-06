package eu.toolchain.rs.processor.annotation;

import eu.toolchain.rs.processor.RsUtils;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import lombok.Data;

@Data
public class SuspendedMirror {
    private final AnnotationMirror annotation;

    public static SuspendedMirror getFor(
            final RsUtils utils, final Element element, final AnnotationMirror a
    ) {
        return new SuspendedMirror(a);
    }
}
