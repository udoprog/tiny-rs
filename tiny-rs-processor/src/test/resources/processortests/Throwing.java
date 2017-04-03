package processortests;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import eu.toolchain.rs.RsInjectBinding;
import eu.toolchain.rs.RsRequestContext;

public interface Throwing {
    @GET
    Object get() throws IOException;
}
