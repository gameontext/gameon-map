package org.gameon.map;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gameon.map.filter.AuthFilter;

import io.swagger.annotations.Api;

@Path("app")
@Api( tags = {"map"})
@Produces(MediaType.APPLICATION_JSON)
public class Application {

    /**
     * GET /map/v1/app/id
     */
    @GET
    @Path("id")
    @io.swagger.annotations.ApiOperation(value = "The unique ID for this map instance",
        notes = "")
    public Response reportID(@Context ServletContext ctx) {
        Object id = ctx.getAttribute(AuthFilter.INSTANCE_ID);
        if(id == null) {
            id = "defaultInstance";
        }
        return Response.ok("{\"id\" : \"" + id.toString() + "\"}").build();
    }

}
