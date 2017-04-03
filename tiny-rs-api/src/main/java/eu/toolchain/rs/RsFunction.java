package eu.toolchain.rs;

/**
 * Local {@link java.util.function.Function} that throws Exception.
 *
 * @param <I> Input type
 * @param <O> Output type
 */
@FunctionalInterface
public interface RsFunction<I, O> {
    O apply(I input) throws Exception;
}
