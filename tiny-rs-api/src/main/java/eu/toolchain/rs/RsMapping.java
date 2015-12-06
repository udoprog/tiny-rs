package eu.toolchain.rs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class RsMapping<T> {
    private final String method;
    private final List<String> path;
    private final Function<RsRequestContext, T> handle;
    private final List<String> consumes;
    private final List<String> produces;

    private RsMapping(final String method, final List<String> path,
            final Function<RsRequestContext, T> handle, final List<String> consumes,
            final List<String> produces) {
        this.method = Objects.requireNonNull(method);
        this.path = Objects.requireNonNull(path);
        this.handle = Objects.requireNonNull(handle);
        this.consumes = Objects.requireNonNull(consumes);
        this.produces = Objects.requireNonNull(produces);
    }

    public String method() {
        return method;
    }

    public List<String> path() {
        return path;
    }

    public Function<RsRequestContext, T> handle() {
        return handle;
    }

    public List<String> consumes() {
        return consumes;
    }

    public List<String> produces() {
        return produces;
    }

    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    public static class Builder<T> {
        private String method;
        private List<String> path = Collections.emptyList();
        private Function<RsRequestContext, T> handle;
        private List<String> consumes = Collections.emptyList();
        private List<String> produces = Collections.emptyList();

        public Builder<T> method(final String method) {
            this.method = Objects.requireNonNull(method);
            return this;
        }

        public Builder<T> path(final String... path) {
            this.path = toList(path);
            return this;
        }

        public Builder<T> handle(final Function<RsRequestContext, T> handle) {
            this.handle = Objects.requireNonNull(handle, "handle");
            return this;
        }

        public Builder<T> voidHandle(final Consumer<RsRequestContext> handle) {
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

        public RsMapping<T> build() {
            return new RsMapping<T>(method, path, handle, consumes, produces);
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
