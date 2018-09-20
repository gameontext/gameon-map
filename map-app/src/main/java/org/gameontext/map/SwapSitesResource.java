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

import java.net.HttpURLConnection;
import java.util.Collection;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gameontext.map.auth.ResourceAccessPolicy;
import org.gameontext.map.auth.ResourceAccessPolicyFactory;
import org.gameontext.map.db.MapRepository;
import org.gameontext.map.model.Site;
import org.gameontext.map.model.SiteSwap;
import org.gameontext.signed.SignedRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Counted;

/**
 * Root of CRUD operations on or with sites
 */
@Path("/swapSites")
@Api( tags = {"map"})
@Produces(MediaType.APPLICATION_JSON)
public class SwapSitesResource {

    @Inject
    private ResourceAccessPolicyFactory resourceAccessPolicyFactory;

    @Inject
    protected MapRepository mapRepository;

    @Context
    protected HttpServletRequest httpRequest;

    private enum AuthMode { AUTHENTICATION_REQUIRED, UNAUTHENTICATED_OK };

	/**
	 * Deprecated: prefer IDEMPOTENT @PUT
     * POST /map/v1/swapSites
     */
    @POST
    @SignedRequest
    @ApiOperation(value="deprecated", hidden=true)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed(name = "put_swapSites_timer",
        reusable = true,
        tags = "label=swapSites")
    @Counted(name = "put_swapSites_count",
        monotonic = true,
        reusable = true,
        tags = "label=swapSites")
    @Metered(name = "put_swapSites_meter",
        reusable = true,
        tags = "label=swapSites")
    public Response swapSites(@QueryParam("room1Id") String room1Id,
            @QueryParam("room2Id") String room2Id) {

        String authenticatedId = getAuthenticatedId(AuthMode.AUTHENTICATION_REQUIRED);
        ResourceAccessPolicy auth = resourceAccessPolicyFactory.createPolicyForUser(authenticatedId);

        // NOTE: Thrown exceptions are mapped (see MapModificationException)
        Collection<Site> mappedRooms = mapRepository.swapRooms(auth, authenticatedId, room1Id, room2Id);
        return Response.ok(mappedRooms).build();
    }

    /**
     * PUT /map/v1/swapSites
     */
    @PUT
    @SignedRequest
    @ApiOperation(value = "Swap the coordinates of two sites",
        notes = "Sites will exchange position in the map, but their contents (including connected players), "
                + "will be undisturbed. The 'exits' be re-assigned.",
        response = Site.class,
        responseContainer = "List",
        code = HttpURLConnection.HTTP_OK,
        hidden=true )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = Messages.BAD_REQUEST),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "swap rooms"),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT)
        })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed(name = "post_swapSites_timer",
        reusable = true,
        tags = "label=swapSites")
    @Counted(name = "post_swapSites_count",
        monotonic = true,
        reusable = true,
        tags = "label=swapSites")
    @Metered(name = "post_swapSites_meter",
        reusable = true,
        tags = "label=swapSites")
    public Response swapSites(
            @ApiParam(value = "Sites to swap", required = true) SiteSwap siteSwap) {

        String authenticatedId = getAuthenticatedId(AuthMode.AUTHENTICATION_REQUIRED);
        ResourceAccessPolicy auth = resourceAccessPolicyFactory.createPolicyForUser(authenticatedId);

        // NOTE: Thrown exceptions are mapped (see MapModificationException)
        Collection<Site> mappedRooms = mapRepository.swapSites(auth, authenticatedId, siteSwap);
        return Response.ok(mappedRooms).build();
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
