package eu.toolchain.rs.processor.result;

import java.util.List;
import java.util.function.Function;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

import com.google.common.collect.ImmutableList;

public interface Result<T> {
    public T get();

    public boolean isOk();

    public void writeError(Messager messager);

    public <O> Result<O> map(Function<? super T, ? extends O> result);

    public <O> Result<O> flatMap(Function<? super T, ? extends Result<O>> result);

    public T orElse(T defaultValue);

    public static <T> Result<T> ok(T reference) {
        return new Ok<>(reference);
    }

    public static <T> Result<T> brokenElement(String message, Element element) {
        return new BrokenElement<>(message, element);
    }

    public static <T> Result<T> brokenAnnotation(String message, Element element,
            AnnotationMirror annotation) {
        return new BrokenAnnotation<>(message, element, annotation);
    }

    public static <T> Result<T> brokenAnnotationValue(String message, Element element,
            AnnotationMirror annotation, AnnotationValue value) {
        return new BrokenAnnotationValue<>(message, element, annotation, value);
    }

    public static <T> Result<List<T>> combine(Iterable<? extends Result<T>> maybes) {
        return new AbstractResult<List<T>>() {
            @Override
            public List<T> get() {
                final ImmutableList.Builder<T> result = ImmutableList.builder();

                for (final Result<T> m : maybes) {
                    result.add(m.get());
                }

                return result.build();
            }

            @Override
            public boolean isOk() {
                boolean verified = true;

                for (final Result<?> m : maybes) {
                    verified = verified && m.isOk();
                }

                return verified;
            }

            @Override
            public void writeError(Messager messager) {
                for (final Result<?> m : maybes) {
                    m.writeError(messager);
                }
            }
        };
    }

    public static Result<?> combineDifferent(Result<?>... maybes) {
        return new AbstractResult<Object>() {
            @Override
            public Object get() {
                return null;
            }

            @Override
            public boolean isOk() {
                boolean verified = true;

                for (final Result<?> m : maybes) {
                    verified = verified && m.isOk();
                }

                return verified;
            }

            @Override
            public void writeError(Messager messager) {
                for (final Result<?> m : maybes) {
                    m.writeError(messager);
                }
            }
        };
    }
}
