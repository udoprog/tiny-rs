package processortests;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import eu.toolchain.rs.RsInjectBinding;
import eu.toolchain.rs.RsRequestContext;

public interface SpecialParameters {
    @GET
    Object requestContext(final RsRequestContext ctx);
}
