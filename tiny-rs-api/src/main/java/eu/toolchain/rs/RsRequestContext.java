package eu.toolchain.rs;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.ws.rs.container.AsyncResponse;

public interface RsRequestContext {
    /**
     * Get the given path parameter.
     *
     * @param key Key of the path parameter to get.
     * @return An optional containing the value of the path parameter, or empty if none exists.
     */
    Optional<RsParameter> getPathParameter(final String key);

    /**
     * Provide a default path parameter.
     *
     * @param key Key of the path parameter provided.
     * @param defaultValue The default value to provide.
     * @return An RsParameter representing the default value.
     */
    default Supplier<RsParameter> provideDefaultPathParameter(String key, String defaultValue) {
        return provideDefault(defaultValue);
    }

    /**
     * Get all values for a given query parameter.
     *
     * @param key Key of the query parameter to get.
     * @return An stream containing all available values of the query parameter.
     */
    Stream<RsParameter> getAllQueryParameters(final String key);

    /**
     * Get the given query parameter.
     *
     * @param key Key of the query parameter to get.
     * @return An optional containing the value of the query parameter, or empty if none exists.
     */
    Optional<RsParameter> getQueryParameter(final String key);

    /**
     * Provide a default query parameter.
     *
     * @param key Key of the query parameter provided.
     * @param defaultValue The default value to provide.
     * @return An RsParameter representing the default value.
     */
    default Supplier<RsParameter> provideDefaultQueryParameter(String key, String defaultValue) {
        return provideDefault(defaultValue);
    }

    /**
     * Get all values for a given header parameter.
     *
     * @param key Key of the header parameter to get.
     * @return An stream containing all available values of the header parameter.
     */
    Stream<RsParameter> getAllHeaderParameters(final String key);

    /**
     * Get the given header parameter.
     *
     * @param key Key of the header parameter to get.
     * @return An optional containing the value of the header parameter, or empty if none exists.
     */
    Optional<RsParameter> getHeaderParameter(final String key);

    /**
     * Provide a default header parameter.
     *
     * @param key Key of the header parameter provided.
     * @param defaultValue The default value to provide.
     * @return An RsParameter representing the default value.
     */
    default Supplier<RsParameter> provideDefaultHeaderParameter(String key, String defaultValue) {
        return provideDefault(defaultValue);
    }

    /**
     * Get the payload of the request.
     *
     * @return An optional containing the payload of the request, or empty if none exists.
     */
    Optional<RsParameter> getPayload();

    /**
     * Get context of the given type.
     *
     * Context are parameters annotated with {@link javax.ws.rs.core.Context}.
     *
     * @param type type of context to get
     * @param <T> type of context
     * @return the context
     * @throws java.lang.IllegalArgumentException if the given context does not exist.
     */
    <T> T getContext(Class<T> type);

    /**
     * Provide a default value as a parameter.
     *
     * @param defaultValue The value to provide.
     * @return A supplier providing a default value.
     */
    Supplier<RsParameter> provideDefault(String defaultValue);

    /**
     * Convert context into a suspended AsyncResponse.
     *
     * @return The suspended Async response.
     */
    default AsyncResponse asSuspended() {
        throw new RsRequestException("suspending requests is not supported");
    }
}
