/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameon.map.v1;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Root of CRUD operations on or with rooms
 */
@Path("/rooms")
@io.swagger.annotations.Api(value = "rooms")
public class RoomsResource {
    
    /**
     * GET /map/v1/rooms
     */
    @GET
    @io.swagger.annotations.ApiOperation(value = "List rooms",
        notes = " Use link headers for pagination.",
        response = RoomInfo.class,
        responseContainer = "List")    
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRooms() {
        // TODO: query/filter parameters, including specification of fields to include in list.
        
        return Response.ok(Collections.emptyList()).build();
    }

    
    /**
     * POST /map/v1/rooms
     */
    @POST
    @io.swagger.annotations.ApiOperation(value = "Create a room",
        notes = "",
        response = RoomInfo.class,
        code = HttpURLConnection.HTTP_CREATED )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRoom(
            @io.swagger.annotations.ApiParam(value = "New room attributes", required = true) RoomAttributes newRoom) {
        return Response.created(URI.create("/map/v1/rooms/1")).entity(new RoomInfo()).build();
    }
    
    /**
     * GET /map/v1/rooms/:id
     */
    @GET
    @Path("{id}")
    @io.swagger.annotations.ApiOperation(value = "Get a specific room",
        notes = "",
        response = RoomInfo.class )
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoom(
            @io.swagger.annotations.ApiParam(value = "Id for target room", required = true) @PathParam("id") String roomId) {
        return Response.ok(new RoomInfo()).build();
    }
    
    
    /**
     * PUT /map/v1/rooms/:id
     */
    @PUT
    @Path("{id}")
    @io.swagger.annotations.ApiOperation(value = "Update a specific room",
        notes = "",
        response = RoomInfo.class )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRoom(
            @io.swagger.annotations.ApiParam(value = "Id for target room", required = true) @PathParam("id") String roomId, 
            @io.swagger.annotations.ApiParam(value = "Updated room attributes", required = true) RoomAttributes newRoom) {
        return Response.ok(new RoomInfo()).build();
    }
    
    
    /**
     * DELETE /map/v1/rooms/:id
     */
    @DELETE
    @Path("{id}")
    @io.swagger.annotations.ApiOperation(value = "Delete a specific room",
        notes = "",
        code = 204 )
    @io.swagger.annotations.ApiResponses(value = {
        @io.swagger.annotations.ApiResponse(code = 204, message = "Delete successful")
    })
    public Response deleteRoom(
            @io.swagger.annotations.ApiParam(value = "Id for target room", required = true) @PathParam("id") String roomId) {
        return Response.noContent().build();
    }
}
