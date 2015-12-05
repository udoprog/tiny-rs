package processortests;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public interface GenericReturnTypeElision {
    @GET
    public List<String> a();

    @GET
    public List<Integer> b();
}
