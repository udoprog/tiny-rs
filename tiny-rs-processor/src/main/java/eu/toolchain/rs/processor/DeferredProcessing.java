package eu.toolchain.rs.processor;

import java.util.Optional;
import java.util.function.Function;

import javax.lang.model.element.TypeElement;

import eu.toolchain.rs.processor.result.Result;
import lombok.Data;

@Data
public class DeferredProcessing {
    private final TypeElement element;
    private final Optional<Result<?>> broken;

    public static Function<DeferredProcessing, DeferredProcessing> refresh(final RsUtils utils) {
        return (d) -> new DeferredProcessing(utils.refetch(d.element), Optional.empty());
    }

    public DeferredProcessing withBroken(final Result<?> broken) {
        return new DeferredProcessing(element, Optional.of(broken));
    }
}
