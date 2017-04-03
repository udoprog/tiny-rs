package processortests;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import java.util.UUID;

import eu.toolchain.rs.RsInjectBinding;
import eu.toolchain.rs.RsRequestContext;

public interface ContextArgument {
    @GET
    Object get(@Context UUID id);
}
