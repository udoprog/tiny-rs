package eu.toolchain.rs;

import java.util.UUID;

public abstract class AsTypeRsParameter implements RsParameter {
    public static final RsTypeReference<Short> SHORT = new RsTypeReference<Short>() {
    };

    public static final RsTypeReference<String> STRING = new RsTypeReference<String>() {
    };

    public static final RsTypeReference<Integer> INTEGER = new RsTypeReference<Integer>() {
    };

    public static final RsTypeReference<Long> LONG = new RsTypeReference<Long>() {
    };

    public static final RsTypeReference<UUID> UUID = new RsTypeReference<UUID>() {
    };

    @Override
    public short asShort() {
        return asType(SHORT);
    }

    @Override
    public String asString() {
        return asType(STRING);
    }

    @Override
    public int asInteger() {
        return asType(INTEGER);
    }

    @Override
    public long asLong() {
        return asType(LONG);
    }

    @Override
    public UUID asUUID() {
        return asType(UUID);
    }
}
