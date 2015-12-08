package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class ConsumesProduces_Binding implements RsRoutesProvider<RsMapping<Object>> {
    private final ConsumesProduces instance;

    public ConsumesProduces_Binding(final ConsumesProduces instance) {
        this.instance = instance;
    }

    public Object a(final RsRequestContext ctx) {
        return instance.a();
    }

    public RsMapping<Object> a_mapping() {
        return RsMapping.<Object>builder().method("GET").handle(this::a).consumes("a", "b").produces("c", "d").build();
    }

    @Override
    public List<RsMapping<Object>> routes() {
        final List<RsMapping<Object>> routes = new ArrayList<>();
        routes.add(a_mapping());
        return routes;
    }
}
