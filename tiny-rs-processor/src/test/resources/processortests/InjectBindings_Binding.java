package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRoutesProvider;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.inject.Inject;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class InjectBindings_Binding implements RsRoutesProvider<RsMapping<?>> {
    private final InjectBindings instance;

    @Inject
    public InjectBindings_Binding(final InjectBindings instance) {
        this.instance = instance;
    }

    @Override
    public List<RsMapping<?>> routes() {
        final List<RsMapping<?>> routes = new ArrayList<>();
        return routes;
    }
}
