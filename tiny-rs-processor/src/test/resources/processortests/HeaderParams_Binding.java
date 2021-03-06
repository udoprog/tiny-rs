package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsMissingHeaderParameter;
import eu.toolchain.rs.RsParameter;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;
import eu.toolchain.rs.RsTypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class HeaderParams_Binding implements RsRoutesProvider<RsMapping<Object>> {
    private static final RsTypeReference<HeaderParams.Interface> TR0 = new RsTypeReference<HeaderParams.Interface>(){};

    private final HeaderParams instance;

    public HeaderParams_Binding(final HeaderParams instance) {
        this.instance = instance;
    }

    public Object a(final RsRequestContext ctx) {
        final String a = ctx.getHeaderParameter("a").orElseThrow(() -> new RsMissingHeaderParameter("a")).asString();
        final Optional<String> b = ctx.getHeaderParameter("b").map(RsParameter::asString);
        final HeaderParams.Interface c = ctx.getHeaderParameter("c").orElseThrow(() -> new RsMissingQueryParameter("c")).asType(TR0);
        final Optional<HeaderParams.Interface> d = ctx.getHeaderParameter("d").map(v -> v.asType(TR0));
        return instance.a(a, b, c, d);
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
