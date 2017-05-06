package eu.toolchain.rs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RsMapping<T> {
    private final String method;
    private final List<String> path;
    private final RsFunction<RsRequestContext, T> handle;
    private final List<String> consumes;
    private final List<String> produces;
    private final RsTypeReference<?> returnType;

    private RsMapping(final String method, final List<String> path,
            final RsFunction<RsRequestContext, T> handle, final List<String> consumes,
            final List<String> produces, final RsTypeReference<?> returnType) {
        this.method = Objects.requireNonNull(method);
        this.path = Objects.requireNonNull(path);
        this.handle = Objects.requireNonNull(handle);
        this.consumes = Objects.requireNonNull(consumes);
        this.produces = Objects.requireNonNull(produces);
        this.returnType = Objects.requireNonNull(returnType);
    }

    public String method() {
        return method;
    }

    public List<String> path() {
        return path;
    }

    public RsFunction<RsRequestContext, T> handle() {
        return handle;
    }

    public List<String> consumes() {
        return consumes;
    }

    public List<String> produces() {
        return produces;
    }

    public RsTypeReference<?> returnType() {
        return returnType;
    }

    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    public static class Builder<T> {
        private String method;
        private List<String> path = Collections.emptyList();
        private RsFunction<RsRequestContext, T> handle;
        private List<String> consumes = Collections.emptyList();
        private List<String> produces = Collections.emptyList();
        private RsTypeReference<?> returnType;

        public Builder<T> method(final String method) {
            this.method = Objects.requireNonNull(method);
            return this;
        }

        public Builder<T> path(final String... path) {
            this.path = toList(path);
            return this;
        }

        public Builder<T> handle(final RsFunction<RsRequestContext, T> handle) {
            this.handle = Objects.requireNonNull(handle, "handle");
            return this;
        }

        public Builder<T> voidHandle(final RsConsumer<RsRequestContext> handle) {
            Objects.requireNonNull(handle, "handle");
            this.handle = ctx -> { handle.accept(ctx); return null; };
            return this;
        }

        public Builder<T> consumes(final String... consumes) {
            this.consumes = toList(consumes);
            return this;
        }

        public Builder<T> produces(final String... produces) {
            this.produces = toList(produces);
            return this;
        }

        public Builder<T> returnType(final RsTypeReference<?> returnType) {
            this.returnType = returnType;
            return this;
        }

        public RsMapping<T> build() {
            return new RsMapping<T>(method, path, handle, consumes, produces, returnType);
        }

        private <I> List<I> toList(final I[] input) {
            final List<I> list = new ArrayList<>();

            for (final I i : input) {
                list.add(i);
            }

            return Collections.unmodifiableList(list);
        }
    }
}
