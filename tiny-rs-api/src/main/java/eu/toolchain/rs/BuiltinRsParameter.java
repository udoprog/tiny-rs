package eu.toolchain.rs;

import java.util.UUID;

/**
 * Delegates built-in string parsing to Java methods (like {@link Integer#parseInt(String)}.
 *
 * @author udoprog
 */
public abstract class BuiltinRsParameter implements RsParameter {
    static final String SHORT_CONVERSION = "not a valid short";
    static final String INTEGER_CONVERSION = "not a valid integer";
    static final String LONG_CONVERSION = "not a valid long";
    static final String UUID_CONVERSION = "not a valid UUID";

    public abstract RuntimeException conversionError(String reason, Object source, Throwable cause);

    @Override
    public short asShort() {
        final String value = asString();

        try {
            return Short.parseShort(value);
        } catch (final NumberFormatException e) {
            throw conversionError(SHORT_CONVERSION, value, e);
        }
    }

    @Override
    public int asInteger() {
        final String value = asString();

        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            throw conversionError(INTEGER_CONVERSION, value, e);
        }
    }

    @Override
    public long asLong() {
        final String value = asString();

        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException e) {
            throw conversionError(LONG_CONVERSION, value, e);
        }
    }

    @Override
    public UUID asUUID() {
        final String value = asString();

        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException e) {
            throw conversionError(UUID_CONVERSION, value, e);
        }
    }
}
