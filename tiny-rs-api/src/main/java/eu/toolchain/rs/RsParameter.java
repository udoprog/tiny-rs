package eu.toolchain.rs;

import java.util.UUID;

/**
 * Represents a 'raw' parameter that can be converted to different types.
 *
 * There are some useful abstract classes available that you can implement for your purpose, like
 * {@link AsTypeRsParameter} and {@link BuiltinRsParameter}.
 *
 * @see AsTypeRsParameter an implementation that delegates all conversion to {@link #asType(Class)}.
 * @see BuiltinRsParameter an implementation that delegates string parsing to built-in methods (like
 *      {{@link Integer#parseInt(String)}.
 * @author udoprog
 */
public interface RsParameter {
    static String INTEGER_CONVERSION = "not a valid integer";
    static String LONG_CONVERSION = "not a valid long";

    /**
     * Convert the given path param to a String.
     *
     * @return The path parameter as a string.
     */
    String asString();

    /**
     * Convert the parameter to a short.
     * @return Short representation of the parameter.
     */
    short asShort();

    /**
     * Convert the parameter to an integer.
     * @return Integer representation of the parameter.
     */
    int asInteger();

    /**
     * Convert the parameter to a long.
     * @return Long representation of the parameter.
     */
    long asLong();

    /**
     * Convert the parameter to a UUID.
     * @return UUID representation of the parameter.
     */
    UUID asUUID();

    /**
     * Convert the parameter to the given type.
     * @param type The type to convert to.
     * @return A representation of the parameter matching the given type.
     */
    <T> T asType(Class<T> type);
}
