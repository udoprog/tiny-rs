package processortests;

import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsRequestContext;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

@Generated("eu.toolchain.rs.processor.RsProcessor")
public class ReturnTypeElision_Binding {
    private final ReturnTypeElision instance;

    public ReturnTypeElision_Binding(final ReturnTypeElision instance) {
        this.instance = instance;
    }

    public int getInteger(final RsRequestContext ctx) {
        return instance.getInteger();
    }

    public RsMapping<Integer> getInteger_mapping() {
        return RsMapping.<Integer>builder().method("GET").handle(this::getInteger).build();
    }

    public long getLong(final RsRequestContext ctx) {
        return instance.getLong();
    }

    public RsMapping<Long> getLong_mapping() {
        return RsMapping.<Long>builder().method("GET").handle(this::getLong).build();
    }

    public List<RsMapping<? extends Number>> routes() {
        final List<RsMapping<? extends Number>> routes = new ArrayList<>();
        routes.add(getInteger_mapping());
        routes.add(getLong_mapping());
        return routes;
    }
}
