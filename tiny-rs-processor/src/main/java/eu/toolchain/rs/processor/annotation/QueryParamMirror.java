package eu.toolchain.rs.processor.annotation;

import eu.toolchain.rs.processor.AnnotationField;
import eu.toolchain.rs.processor.AnnotationValues;
import eu.toolchain.rs.processor.RsUtils;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import lombok.Data;

@Data
public class QueryParamMirror {
    private final AnnotationMirror annotation;
    private final String value;

    public static QueryParamMirror getFor(
            final RsUtils utils, final Element element, final AnnotationMirror a
    ) {
        final AnnotationValues values = utils.getElementValuesWithDefaults(element, a);

        final String value = values
                .get("value")
                .map(AnnotationField::asString)
                .orElseThrow(() -> new IllegalArgumentException("value"));

        return new QueryParamMirror(a, value);
    }
}
