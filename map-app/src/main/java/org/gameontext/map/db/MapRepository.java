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
package org.gameontext.map.db;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.gameontext.map.Log;
import org.gameontext.map.MapModificationException;
import org.gameontext.map.auth.ResourceAccessPolicy;
import org.gameontext.map.auth.SiteSwapPermission;
import org.gameontext.map.clients.Kafka;
import org.gameontext.map.clients.Kafka.SiteEvent;
import org.gameontext.map.model.ConnectionDetails;
import org.gameontext.map.model.Coordinates;
import org.gameontext.map.model.Exits;
import org.gameontext.map.model.RoomInfo;
import org.gameontext.map.model.Site;
import org.gameontext.map.model.SiteSwap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;


@ApplicationScoped
public class MapRepository {

    @Inject
    protected CouchDbConnector db;

    protected SiteDocuments sites;

    protected ObjectMapper mapper;

    @Inject
    Kafka kafka;

    @PostConstruct
    protected void postConstruct() {
        // Create an ObjectMapper for marshalling responses back to REST clients
        mapper = new ObjectMapper();

        try {
            // Ensure required views exist
            sites = new SiteDocuments(db);

            // Make sure that first room has neighbors (should always do, but.. )
            Exits exits = new Exits();
            sites.createEmptyNeighbors(new Coordinates(0, 0), exits);
        } catch (Exception e) {
            // Log the warning, and then re-throw to prevent this class from going into service,
            // which will prevent injection to the Health check, which will make the app stay down.
            Log.log(Level.WARNING, this, "Unable to connect to database", e);
            throw e;
        }
    }

    public boolean connectionReady() {
        return true;
    }

    /**
     * List of all not-empty rooms
     * @param user the id of the person requesting the list, or null if unauthenticated
     * @param map
     * @return List of all sites, possibly filtered by owner and/or name. Will not return null.
     */
    public List<JsonNode> listSites(ResourceAccessPolicy accessPolicy, String owner, String name) {
        Log.log(Level.FINER, this, "List all rooms");

        List<JsonNode> result = sites.listSites(nullEmpty(owner), nullEmpty(name));

        //we have to step through any results to remove the connectionDetails blocks.
        for(JsonNode j : result){
            JsonNode ownerNode = j.get("owner");
            if(ownerNode!=null && ownerNode.getNodeType().equals(JsonNodeType.STRING)){
                String ownerNodeString = ownerNode.textValue();
                //remove connectionDetailsBlocks unless requested by owner or the system id
                if( stripSensitiveData(accessPolicy, ownerNodeString)){
                    JsonNode info = j.get("info");
                    if(info.getNodeType() == JsonNodeType.OBJECT){
                        ObjectNode infoObj = (ObjectNode)info;
                        if(infoObj.has("connectionDetails")){
                            infoObj.remove("connectionDetails");
                        }
                    }
                }
            }
        }
        return result;
    }

    private String nullEmpty(String parameter) {
        if ( parameter != null && parameter.trim().isEmpty() ) {
            return null;
        }
        return parameter;
    }

    /**
     * Connect/Add/Place a new room into the Map
     * @param user the person adding the new room
     * @param newRoom Room or Suite to add
     * @return Wired node containing the room or Suite
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site connectRoom(String user, RoomInfo newRoom) {
        Log.log(Level.FINER, this, "Add new site: {0} by {1}", newRoom.getName(), user);

        // TODO: Revisit this when we have groups/organizations.. *sigh*
        if ( user == null ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room could not be created",
                    "Owner was not specified (unauthenticated)");
        }

        // TODO: Input validation for connection details (most important)

        Site result = sites.connectRoom(user, newRoom);

        //publish event
        kafka.publishSiteEvent(SiteEvent.CREATE, result);

        return result;
    }

    /**
     * Connect/Add/Place a new room into the Map
     * @param user the person adding the new room
     * @param room id to attempt
     * @param newRoom Room or Suite to add
     * @return Wired node containing the room or Suite
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site connectRoom(String user, String id, RoomInfo newRoom) {
        Log.log(Level.FINER, this, "Add new site: {0} by {1}", newRoom.getName(), user);

        // TODO: Revisit this when we have groups/organizations.. *sigh*
        if ( user == null ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room could not be created",
                    "Owner was not specified (unauthenticated)");
        }

        // TODO: Input validation for connection details (most important)
        // TODO: Input validation for proposed id (URL friendly)

        Site result = sites.connectRoomWithId(user, id, newRoom);

        //publish event
        kafka.publishSiteEvent(SiteEvent.CREATE, result);

        return result;
    }

    /**
     * Get room by id
     *
     * @param user person requesting the room, or null if unauthenticated
     * @param id Site/Room id
     * @return Complete information for the specified room/site
     * @throws DocumentNotFoundException for unknown room
     */
    public Site getRoom(ResourceAccessPolicy accessPolicy, String id) {
        Log.log(Level.FINER, this, "Lookup site: {0}", id);

        Site result = sites.getSite(id);
        String owner = result.getOwner();

        if( stripSensitiveData(accessPolicy, owner) ){
            // Remove connection details block (except for owner or system id)
            if(result.getInfo()!=null && result.getInfo().getConnectionDetails()!=null){
                result.getInfo().setConnectionDetails(null);
            }
        }

        return result;
    }

    /**
     * Update room by id
     *
     * @param authenticatedId person attempting the update.
     * @param id Site/Room id
     * @param updatedInfo Updated room information
     * @return Complete information for the specified room/site
     * @throws JsonProcessingException
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site updateRoom(String authenticatedId, String id, RoomInfo roomInfo) {
        Log.log(Level.FINER, this, "Update site: {0} {1}", id, roomInfo);

        if ( authenticatedId == null ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room could not be updated",
                    "User was not specified (unauthenticated)");
        }

        Site result = sites.updateRoom(authenticatedId, id, roomInfo);

        //publish event.
        kafka.publishSiteEvent(SiteEvent.UPDATE, result);

        return result;
    }

    /**
     * Swap to rooms around
     * @param room1Id First site in swap
     * @param room2Id Second site in swap
     */
    public Collection<Site> swapRooms(ResourceAccessPolicy accessPolicy, String user, String room1Id, String room2Id) {
        Log.log(Level.FINER, this, "Swap rooms: {0} {1}", room1Id, room2Id);

        if (accessPolicy == null || !accessPolicy.isAuthorized(null, SiteSwapPermission.class)) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "User " + user + " does not have permission to swap rooms.",
                    "Rooms " + room1Id + " and " + room2Id + " have not been swapped.");
        }
        Collection<Site> results = sites.swapRooms(room1Id, room2Id);

        //publish events
        results.forEach(site -> kafka.publishSiteEvent(SiteEvent.UPDATE, site));

        return results;
    }

    public List<Site> swapSites(ResourceAccessPolicy accessPolicy, String user, SiteSwap siteSwap) {
        Log.log(Level.FINER, this, "Swap sites: {0} {1}", siteSwap.getSite1().getId(), siteSwap.getSite2().getId());

        if (accessPolicy == null || !accessPolicy.isAuthorized(null, SiteSwapPermission.class)) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "User " + user + " does not have permission to swap rooms.",
                    "Sites " + siteSwap.getSite1().getId() + " and " + siteSwap.getSite2().getId() + " have not been swapped.");
        }
        List<Site> results = sites.swapSites(siteSwap);

        //publish events
        results.forEach(site -> kafka.publishSiteEvent(SiteEvent.UPDATE, site));

        return results;
    }

    /**
     * Delete site by id
     * @param authenticatedId person attempting the delete
     * @param id Site/Room id
     */
    public void deleteSite(String authenticatedId, String id) {
        Log.log(Level.FINER, this, "Delete site {0} by {1}", id, authenticatedId);

        sites.deleteSite(authenticatedId, id);

        //publish event..
        //we don't have a full site to send, but notifying by id is fine.
        Site deleted = new Site();
        deleted.setId(id);
        kafka.publishSiteEvent(SiteEvent.DELETE, deleted);

    }

    public ObjectMapper mapper() {
        return mapper;
    }

    private boolean stripSensitiveData(ResourceAccessPolicy accessPolicy, String owner) {
        return !accessPolicy.isAuthorized(owner, ConnectionDetails.class);
    }

}
