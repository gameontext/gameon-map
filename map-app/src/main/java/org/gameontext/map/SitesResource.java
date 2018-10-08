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
package org.gameontext.map;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.gameontext.map.auth.ResourceAccessPolicy;
import org.gameontext.map.auth.ResourceAccessPolicyFactory;
import org.gameontext.map.db.MapRepository;
import org.gameontext.map.model.ErrorResponse;
import org.gameontext.map.model.RoomInfo;
import org.gameontext.map.model.Site;
import org.gameontext.signed.SignedRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Root of CRUD operations on or with sites
 */
@Path("/sites")
@Api( tags = {"map"})
@Produces(MediaType.APPLICATION_JSON)
public class SitesResource {

    @Inject
    private ResourceAccessPolicyFactory resourceAccessPolicyFactory;

    @Inject
    protected MapRepository mapRepository;

    @Context
    protected HttpServletRequest httpRequest;

    private enum AuthMode { AUTHENTICATION_REQUIRED, UNAUTHENTICATED_OK };

    /**
     * GET /map/v1/sites
     */
    @GET
    @SignedRequest
    @ApiOperation(value = "List sites",
        notes = "Get a list of registered sites. Use link headers for pagination.",
        response = Site.class,
        responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL,
                    response = Site.class, responseContainer = "List"),
            @ApiResponse(code = HttpServletResponse.SC_NO_CONTENT, message = Messages.NOT_FOUND)
        })
    @Produces(MediaType.APPLICATION_JSON)
    @Timed(name = "listAll_timer",
        reusable = true,
        tags = "label=listAll")
    @Counted(name = "listAll_count",
        monotonic = true,
        reusable = true,
        tags = "label=listAll")
    @Metered(name = "listAll_meter",
        reusable = true,
        tags = "label=listAll")
    @Fallback(fallbackMethod="listAllFallback")
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, maxDuration= 10000)
    public Response listAll(
            @ApiParam(value = "filter by owner") @QueryParam("owner") String owner,
            @ApiParam(value = "filter by name") @QueryParam("name") String name) {

        String authenticatedId = getAuthenticatedId(AuthMode.UNAUTHENTICATED_OK);
        ResourceAccessPolicy auth = resourceAccessPolicyFactory.createPolicyForUser(authenticatedId);

        // TODO: pagination,  fields to include in list (i.e. just Exits).
        List<JsonNode> sites = mapRepository.listSites(auth, owner, name);

        if ( sites.isEmpty() )
            return Response.noContent().build();
        else {
            // TODO -- this should be done better. Stream, something.
            return Response.ok().entity(sites.toString()).build();
        }
    }

    public Response listAllFallback(
            @ApiParam(value = "filter by owner") @QueryParam("owner") String owner,
            @ApiParam(value = "filter by name") @QueryParam("name") String name) {
        return Response.noContent().build();
    }

	/**
     * POST /map/v1/sites
     * @throws JsonProcessingException
     */
    @POST
    @SignedRequest
    @ApiOperation(value = "Create a room",
        notes = "When a room is registered, the map will generate the appropriate paths to "
                + "place the room into the map. The map wll only generate links using standard 2-d "
                + "compass directions. The 'exits' attribute in the return value describes "
                + "connected/adjacent sites. ",
        response = Site.class,
        code = HttpServletResponse.SC_CREATED )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_CREATED, message = Messages.SUCCESSFUL, response = Site.class),
            @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = Messages.BAD_REQUEST, response = ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "create room", response = ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response = ErrorResponse.class)
        })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed(name = "createRoom_timer",
        reusable = true,
        tags = "label=createRoom")
    @Counted(name = "createRoom_count",
        monotonic = true,
        reusable = true,
        tags = "label=createRoom")
    @Metered(name = "createRoom_meter",
        reusable = true,
        tags = "label=createRoom")
    public Response createRoom(
            @ApiParam(value = "New room attributes", required = true) RoomInfo newRoom) {

        // NOTE: Thrown exeptions are mapped (see MapModificationException)
        Site mappedRoom = mapRepository.connectRoom(getAuthenticatedId(AuthMode.AUTHENTICATION_REQUIRED), newRoom);

        return Response.created(URI.create("/map/v1/sites/" + mappedRoom.getId())).entity(mappedRoom).build();
    }

    /**
     * GET /map/v1/sites/:id
     * @throws JsonProcessingException
     */
    @GET
    @SignedRequest
    @Path("{id}")
    @ApiOperation(value = "Get a specific room",
        notes = "",
        response = Site.class )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, response = Site.class),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response = ErrorResponse.class)
        })
    @Produces(MediaType.APPLICATION_JSON)
    @Timed(name = "getRoom_timer",
        reusable = true,
        tags = "label=getRoom")
    @Counted(name = "getRoom_count",
        monotonic = true,
        reusable = true,
        tags = "label=getRoom")
    @Metered(name = "getRoom_meter",
        reusable = true,
        tags = "label=getRoom")
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, maxDuration= 10000)
    public Response getRoom(
            @ApiParam(value = "target room id", required = true) @PathParam("id") String roomId) {
        String authenticatedId = getAuthenticatedId(AuthMode.UNAUTHENTICATED_OK);
        ResourceAccessPolicy auth = resourceAccessPolicyFactory.createPolicyForUser(authenticatedId);

        Site mappedRoom = mapRepository.getRoom(auth,roomId);
        return Response.ok(mappedRoom).build();
    }


    /**
     * PUT /map/v1/sites/:id
     * @throws JsonProcessingException
     */
    @PUT
    @SignedRequest
    @Path("{id}")
    @ApiOperation(value = "Update a specific room",
        notes = "",
        response = Site.class )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, response = Site.class),
            @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = Messages.BAD_REQUEST, response = ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "update room", response = ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response = ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response = ErrorResponse.class)
        })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed(name = "updateRoom_timer",
        reusable = true,
        tags = "label=updateRoom")
    @Counted(name = "updateRoom_count",
        monotonic = true,
        reusable = true,
        tags = "label=updateRoom")
    @Metered(name = "updateRoom_meter",
        reusable = true,
        tags = "label=updateRoom")
    public Response updateRoom(
            @ApiParam(value = "target room id", required = true) @PathParam("id") String roomId,
            @ApiParam(value = "Updated room attributes", required = true) RoomInfo roomInfo) {

        Site mappedRoom = mapRepository.updateRoom(getAuthenticatedId(AuthMode.AUTHENTICATION_REQUIRED), roomId, roomInfo);
        return Response.ok(mappedRoom).build();
    }


    /**
     * DELETE /map/v1/sites/:id
     */
    @DELETE
    @SignedRequest
    @Path("{id}")
    @ApiOperation(value = "Delete a specific room",
        notes = "",
        code = 204 )
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_NO_CONTENT, message = Messages.SUCCESSFUL),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = Messages.BAD_REQUEST, response = ErrorResponse.class),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "delete room", response = ErrorResponse.class),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response = ErrorResponse.class),
        @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response = ErrorResponse.class)
    })
    @Timed(name = "deleteRoom_timer",
        reusable = true,
        tags = "label=deleteRoom")
    @Counted(name = "deleteRoom_count",
        monotonic = true,
        reusable = true,
        tags = "label=deleteRoom")
    @Metered(name = "deleteRoom_meter",
        reusable = true,
        tags = "label=deleteRoom")
    public Response deleteRoom(
            @ApiParam(value = "target room id", required = true) @PathParam("id") String roomId) {

        mapRepository.deleteSite(getAuthenticatedId(AuthMode.AUTHENTICATION_REQUIRED), roomId);
        return Response.noContent().build();
    }

    private String getAuthenticatedId(AuthMode mode){
        // This attribute will be set by the auth filter when a user has made
        // an authenticated request.
        String authedId = (String) httpRequest.getAttribute("player.id");
        switch(mode){
            case AUTHENTICATION_REQUIRED:{
                if (authedId == null || authedId.isEmpty()) {
                    //else we don't allow unauthenticated, so if auth id is absent
                    //throw exception to prevent handling the request.
                    throw new MapModificationException(Response.Status.BAD_REQUEST,
                        "Unauthenticated client", "Room owner could not be determined.");
                }
                break;
            }
            case UNAUTHENTICATED_OK:{
                //if we allow unauthenticated, we will clean up so null==unauthed.
                if(authedId!=null && authedId.isEmpty()){
                    authedId = null;
                }
                break;
            }
        }
        return authedId;

    }

}
