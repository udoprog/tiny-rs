package processortests;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;

public interface DefaultValues {
    @GET
    public Object a(@QueryParam("a") @DefaultValue("foo") final String a);

    public interface Interface {
    }
}
