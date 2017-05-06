package eu.toolchain.rs;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class RsTypeReference<T> {
    protected final Type type;

    protected RsTypeReference() {
        type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }
}
