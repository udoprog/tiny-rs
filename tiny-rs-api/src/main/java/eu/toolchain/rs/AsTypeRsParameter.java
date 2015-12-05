package eu.toolchain.rs;

import java.util.UUID;

public abstract class AsTypeRsParameter implements RsParameter {
    @Override
    public short asShort() {
        return asType(Short.class);
    }

    @Override
    public String asString() {
        return asType(String.class);
    }

    @Override
    public int asInteger() {
        return asType(Integer.class);
    }

    @Override
    public long asLong() {
        return asType(Long.class);
    }

    @Override
    public UUID asUUID() {
        return asType(UUID.class);
    }
}
