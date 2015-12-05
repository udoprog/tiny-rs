package processortests;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.QueryParam;
import javax.ws.rs.GET;

public interface QueryParams {
    @GET
    public Object a(@QueryParam("a") final String a, @QueryParam("b") Optional<String> b,
            @QueryParam("c") Interface c, @QueryParam("d") Optional<Interface> d,
            @QueryParam("all") List<String> all, @QueryParam("allTyped") List<Interface> allTyped);

    public interface Interface {
    }
}
