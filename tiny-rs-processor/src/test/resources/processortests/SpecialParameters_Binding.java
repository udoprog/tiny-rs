package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;
import eu.toolchain.rs.RsTypeReference;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.ws.rs.container.AsyncResponse;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class SpecialParameters_Binding implements RsRoutesProvider<RsMapping<?>> {
    private final SpecialParameters instance;

    public SpecialParameters_Binding(final SpecialParameters instance) {
        this.instance = instance;
    }

    public Object requestContext(final RsRequestContext ctx) {
        return instance.requestContext(ctx);
    }

    public RsMapping<Object> requestContext_mapping() {
        return RsMapping.<Object>builder().method("GET").handle(this::requestContext).returnType(new RsTypeReference<Object>(){}).build();
    }

    public void suspended(final RsRequestContext ctx) {
        final AsyncResponse async = ctx.asSuspended();
        instance.suspended(async);
    }

    public RsMapping<Void> suspended_mapping() {
        return RsMapping.<Void>builder().method("GET").voidHandle(this::suspended).returnType(new RsTypeReference<Void>(){}).build();
    }

    @Override
    public List<RsMapping<?>> routes() {
        final List<RsMapping<?>> routes = new ArrayList<>();
        routes.add(requestContext_mapping());
        routes.add(suspended_mapping());
        return routes;
    }
}

