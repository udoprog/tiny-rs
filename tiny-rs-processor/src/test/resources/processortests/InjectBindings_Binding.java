package processortests;

import eu.toolchain.rs.RsMapping;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.inject.Inject;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class InjectBindings_Binding {
    private final InjectBindings instance;

    @Inject
    public InjectBindings_Binding(final InjectBindings instance) {
        this.instance = instance;
    }

    public List<RsMapping<?>> routes() {
        final List<RsMapping<?>> routes = new ArrayList<>();
        return routes;
    }
}
