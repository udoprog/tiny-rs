package eu.toolchain.rs;

/**
 * Local {@link java.util.function.Consumer} that throws Exception.
 *
 * @param <T> Consumed type
 */
@FunctionalInterface
public interface RsConsumer<T> {
    void accept(T input) throws Exception;
}
