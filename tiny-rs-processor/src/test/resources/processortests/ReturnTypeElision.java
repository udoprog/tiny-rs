package processortests;

import javax.ws.rs.GET;

public interface ReturnTypeElision {
    @GET
    public int getInteger();

    @GET
    public long getLong();
}
