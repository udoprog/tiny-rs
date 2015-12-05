package eu.toolchain.rs.processor.result;

import javax.annotation.processing.Messager;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(of = {"reference"}, callSuper = false)
public class Ok<T> extends AbstractResult<T> {
    final T reference;

    public T get() {
        return reference;
    }

    @Override
    public boolean isOk() {
        return true;
    }

    @Override
    public void writeError(Messager messager) {
    }
}
