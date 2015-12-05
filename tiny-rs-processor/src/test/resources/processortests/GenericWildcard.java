package processortests;

import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public interface GenericWildcard {
    @GET
    public Optional<?> test();
}
