package eu.toolchain.rs;

import java.util.List;

public interface RsRoutesProvider<T> {
    List<T> routes();
}
