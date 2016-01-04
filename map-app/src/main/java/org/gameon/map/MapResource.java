package org.gameon.map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class MapResource {
    
    @GET
    public Response basicGet() {
        return Response.ok().build();
    }
}
