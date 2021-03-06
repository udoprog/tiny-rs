package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsMissingQueryParameter;
import eu.toolchain.rs.RsParameter;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;

import eu.toolchain.rs.RsTypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class QueryParams_Binding implements RsRoutesProvider<RsMapping<Object>> {
    private static final RsTypeReference<QueryParams.Interface> TR0 = new RsTypeReference<QueryParams.Interface>(){};

    private final QueryParams instance;

    public QueryParams_Binding(final QueryParams instance) {
        this.instance = instance;
    }

    public Object a(final RsRequestContext ctx) {
        final String a = ctx.getQueryParameter("a").orElseThrow(() -> new RsMissingQueryParameter("a")).asString();
        final Optional<String> b = ctx.getQueryParameter("b").map(RsParameter::asString);
        final QueryParams.Interface c = ctx.getQueryParameter("c").orElseThrow(() -> new RsMissingQueryParameter("c")).asType(TR0);
        final Optional<QueryParams.Interface> d = ctx.getQueryParameter("d").map(v -> v.asType(new RsTypeReference<QueryParams.Interface>(){}));
        final List<String> all = ctx.getAllQueryParameters("all").map(RsParameter::asString).collect(Collectors.toList());
        final List<QueryParams.Interface> allTyped = ctx.getAllQueryParameters("allTyped").map(v -> v.asType(TR0)).collect(Collectors.toList());
        return instance.a(a, b, c, d, all, allTyped);
    }

    public RsMapping<Object> a_mapping() {
        return RsMapping.<Object>builder().method("GET").handle(this::a).returnType(new RsTypeReference<Object>(){}).build();
    }

    @Override
    public List<RsMapping<Object>> routes() {
        final List<RsMapping<Object>> routes = new ArrayList<>();
        routes.add(a_mapping());
        return routes;
    }
}
