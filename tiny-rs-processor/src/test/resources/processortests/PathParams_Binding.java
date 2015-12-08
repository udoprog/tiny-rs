package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsMissingPathParameter;
import eu.toolchain.rs.RsParameter;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class PathParams_Binding implements RsRoutesProvider<RsMapping<Object>> {
    private final PathParams instance;

    public PathParams_Binding(final PathParams instance) {
        this.instance = instance;
    }

    public Object a(final RsRequestContext ctx) {
        final String a = ctx.getPathParameter("a").orElseThrow(() -> new RsMissingPathParameter("a")).asString();
        final Optional<String> b = ctx.getPathParameter("b").map(RsParameter::asString);
        final PathParams.Interface c = ctx.getPathParameter("c").orElseThrow(() -> new RsMissingQueryParameter("c")).asType(PathParams.Interface.class);
        final Optional<PathParams.Interface> d = ctx.getPathParameter("d").map(v -> v.asType(PathParams.Interface.class));
        return instance.a(a, b, c, d);
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
