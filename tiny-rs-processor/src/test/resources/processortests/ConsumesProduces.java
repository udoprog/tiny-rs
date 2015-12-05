package processortests;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

public interface ConsumesProduces {
    @GET
    @Consumes({"a", "b"})
    @Produces({"c", "d"})
    Object a();
}
