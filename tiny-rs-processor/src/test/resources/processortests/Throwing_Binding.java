package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;
import eu.toolchain.rs.RsTypeReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class Throwing_Binding implements RsRoutesProvider<RsMapping<Object>> {
    private final Throwing instance;

    public Throwing_Binding(final Throwing instance) {
        this.instance = instance;
    }

    public Object get(final RsRequestContext ctx) throws IOException {
        return instance.get();
    }

    public RsMapping<Object> get_mapping() {
        return RsMapping.<Object>builder().method("GET").handle(this::get).returnType(new RsTypeReference<Object>(){}).build();
    }

    @Override
    public List<RsMapping<Object>> routes() {
        final List<RsMapping<Object>> routes = new ArrayList<>();
        routes.add(get_mapping());
        return routes;
    }
}

