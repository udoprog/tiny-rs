package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;
import eu.toolchain.rs.RsTypeReference;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class Payload_Binding implements RsRoutesProvider<RsMapping<List<String>>> {
    private final Payload instance;

    public Payload_Binding(final Payload instance) {
        this.instance = instance;
    }

    public List<String> get(final RsRequestContext ctx) {
        return instance.get();
    }

    public RsMapping<List<String>> get_mapping() {
        return RsMapping.<List<String>>builder().method("GET").handle(this::get).returnType(new RsTypeReference<List<String>>(){}).build();
    }

    @Override
    public List<RsMapping<List<String>>> routes() {
        final List<RsMapping<List<String>>> routes = new ArrayList<>();
        routes.add(get_mapping());
        return routes;
    }
}

