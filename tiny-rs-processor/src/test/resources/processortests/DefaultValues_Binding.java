package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class DefaultValues_Binding implements RsRoutesProvider<RsMapping<Object>> {
    private final DefaultValues instance;

    public DefaultValues_Binding(final DefaultValues instance) {
        this.instance = instance;
    }

    public Object a(final RsRequestContext ctx) {
        final String a = ctx.getQueryParameter("a").orElseGet(ctx.provideDefaultQueryParameter("a", "foo")).asString();
        return instance.a(a);
    }

    public RsMapping<Object> a_mapping() {
        return RsMapping.<Object>builder().method("GET").handle(this::a).build();
    }

    @Override
    public List<RsMapping<Object>> routes() {
        final List<RsMapping<Object>> routes = new ArrayList<>();
        routes.add(a_mapping());
        return routes;
    }
}
