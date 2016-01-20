package org.gameon.map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.gameon.map.couchdb.MapRepository;

@Path("health")
public class Health {

    @Inject
    protected MapRepository mapRepository;

    /**
     * GET /map/v1/health
     */
    @GET
    @io.swagger.annotations.ApiOperation(value = "Check application health",
        notes = "")
    public Response healthCheck() {
        if ( mapRepository != null ) {
            return Response.ok().build();
        } else {
            return Response.serverError().build();
        }
    }

}
