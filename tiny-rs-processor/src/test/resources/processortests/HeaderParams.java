package processortests;

import java.util.Optional;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.GET;

public interface HeaderParams {
    @GET
    public Object a(@HeaderParam("a") final String a, @HeaderParam("b") Optional<String> b,
            @HeaderParam("c") Interface c, @HeaderParam("d") Optional<Interface> d);

    public interface Interface {
    }
}
