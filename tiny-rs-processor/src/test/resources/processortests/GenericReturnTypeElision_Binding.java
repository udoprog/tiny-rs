package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class GenericReturnTypeElision_Binding implements RsRoutesProvider<RsMapping<? extends List<?>>> {
    private final GenericReturnTypeElision instance;

    public GenericReturnTypeElision_Binding(final GenericReturnTypeElision instance) {
        this.instance = instance;
    }

    public List<String> a(final RsRequestContext ctx) {
        return instance.a();
    }

    public RsMapping<List<String>> a_mapping() {
        return RsMapping.<List<String>>builder().method("GET").handle(this::a).build();
    }

    public List<Integer> b(final RsRequestContext ctx) {
        return instance.b();
    }

    public RsMapping<List<Integer>> b_mapping() {
        return RsMapping.<List<Integer>>builder().method("GET").handle(this::b).build();
    }

    @Override
    public List<RsMapping<? extends List<?>>> routes() {
        final List<RsMapping<? extends List<?>>> routes = new ArrayList<>();
        routes.add(a_mapping());
        routes.add(b_mapping());
        return routes;
    }
}
