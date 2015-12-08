package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class GenericWildcard_Binding implements RsRoutesProvider<RsMapping<Optional<?>>> {
    private final GenericWildcard instance;

    public GenericWildcard_Binding(final GenericWildcard instance) {
        this.instance = instance;
    }

    public Optional<?> test(final RsRequestContext ctx) {
        return instance.test();
    }

    public RsMapping<Optional<?>> test_mapping() {
        return RsMapping.<Optional<?>>builder().method("GET").handle(this::test).build();
    }

    @Override
    public List<RsMapping<Optional<?>>> routes() {
        final List<RsMapping<Optional<?>>> routes = new ArrayList<>();
        routes.add(test_mapping());
        return routes;
    }
}
