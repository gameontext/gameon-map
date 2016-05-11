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
package net.wasdev.gameon.map;

import java.net.HttpURLConnection;
import java.util.Collection;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.wasdev.gameon.map.couchdb.MapRepository;
import net.wasdev.gameon.map.couchdb.auth.ResourceAccessPolicy;
import net.wasdev.gameon.map.couchdb.auth.ResourceAccessPolicyFactory;
import net.wasdev.gameon.map.models.Site;

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
     * POST /map/v1/swapSites
     */
    @POST
    @ApiOperation(value = "Swap two sites over",
        notes = "When two sites are swapped the contents of the room and any people in the "
                + "room will move with the site. The 'exits' then get re-assigned.",
        response = Collection.class,
        code = HttpURLConnection.HTTP_OK )
    @Produces(MediaType.APPLICATION_JSON)
    public Response swapSites(
            @ApiParam(value = "Id of first room to swap", required = true) @QueryParam("room1Id") String room1Id,
            @ApiParam(value = "Id of second room to swap", required = true) @QueryParam("room2Id") String room2Id)
    {
        String authenticatedId = getAuthenticatedId(AuthMode.AUTHENTICATION_REQUIRED);
        ResourceAccessPolicy auth = resourceAccessPolicyFactory.createPolicyForUser(authenticatedId);
        
        // NOTE: Thrown exceptions are mapped (see MapModificationException)
        Collection<Site> mappedRooms = mapRepository.swapRooms(auth, authenticatedId, room1Id, room2Id);
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
