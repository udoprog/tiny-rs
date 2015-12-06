package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class SpecialParameters_Binding {
    private final SpecialParameters instance;

    public SpecialParameters_Binding(final SpecialParameters instance) {
        this.instance = instance;
    }

    public Object requestContext(final RsRequestContext ctx) {
        return instance.requestContext(ctx);
    }

    public RsMapping<Object> requestContext_mapping() {
        return RsMapping.<Object>builder().method("GET").handle(this::requestContext).build();
    }

    public List<RsMapping<Object>> routes() {
        final List<RsMapping<Object>> routes = new ArrayList<>();
        routes.add(requestContext_mapping());
        return routes;
    }
}
