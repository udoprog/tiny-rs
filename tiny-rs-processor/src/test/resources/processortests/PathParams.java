package processortests;

import java.util.Optional;

import javax.ws.rs.PathParam;
import javax.ws.rs.GET;

public interface PathParams {
    @GET
    public Object a(@PathParam("a") final String a, @PathParam("b") Optional<String> b,
            @PathParam("c") Interface c, @PathParam("d") Optional<Interface> d);

    public interface Interface {
    }
}
