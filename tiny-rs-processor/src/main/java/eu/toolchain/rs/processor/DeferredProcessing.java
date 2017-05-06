package eu.toolchain.rs.processor;

import java.util.Optional;
import java.util.function.Function;
import javax.lang.model.element.TypeElement;
import lombok.Data;

@Data
public class DeferredProcessing {
    private final TypeElement element;
    private final Optional<BrokenElement> error;

    public static Function<? super DeferredProcessing, ? extends DeferredProcessing> refresh(
            final RsUtils utils
    ) {
        return processing -> new DeferredProcessing(utils.refetch(processing.element),
                Optional.empty());
    }
}
