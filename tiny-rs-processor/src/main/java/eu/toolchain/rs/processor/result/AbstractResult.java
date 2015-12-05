package eu.toolchain.rs.processor.result;

import java.util.function.Function;

import javax.annotation.processing.Messager;

public abstract class AbstractResult<T> implements Result<T> {
    @Override
    public <O> Result<O> map(Function<? super T, ? extends O> result) {
        if (isOk()) {
            return Result.ok(result.apply(get()));
        }

        return new AbstractResult<O>() {
            @Override
            public O get() {
                throw new IllegalStateException("broken");
            }

            @Override
            public boolean isOk() {
                return false;
            }

            @Override
            public void writeError(Messager messager) {
                AbstractResult.this.writeError(messager);
            }
        };
    }

    @Override
    public <O> Result<O> flatMap(Function<? super T, ? extends Result<O>> result) {
        if (isOk()) {
            return result.apply(get());
        }

        return new AbstractResult<O>() {
            @Override
            public O get() {
                throw new IllegalStateException("broken");
            }

            @Override
            public boolean isOk() {
                return false;
            }

            @Override
            public void writeError(Messager messager) {
                AbstractResult.this.writeError(messager);
            }
        };
    }

    @Override
    public T orElse(T defaultValue) {
        if (isOk()) {
            return get();
        }

        return defaultValue;
    }
}
