package org.gameon.map.couchdb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.gameon.map.Log;
import org.gameon.map.MapModificationException;
import org.gameon.map.models.Coordinates;
import org.gameon.map.models.Exits;
import org.gameon.map.models.RoomInfo;
import org.gameon.map.models.Site;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public class MapRepository {

    private static final String GAME_ON_ORG = "game-on.org";

    @Resource(name = "couchdb/connector")
    protected CouchDbInstance db;

    protected SiteDocuments sites;

    protected ObjectMapper mapper;

    @PostConstruct
    protected void postConstruct() {
        String dbname = "map_repository";

        // Create an ObjectMapper for marshalling responses back to REST clients
        mapper = new ObjectMapper();

        try {
            // Connect to the database with the specified
            CouchDbConnector dbc = db.createConnector(dbname, false);
            Log.log(Level.INFO, this, "Connected to {0}", dbname);

            // Ensure required views exist
            sites = new SiteDocuments(dbc);

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
     * @param authenticatedId the id of the person requesting the list, or null if unauthenticated
     * @param map
     * @return List of all sites, possibly filtered by owner and/or name. Will not return null.
     */
    public List<JsonNode> listSites(String authenticatedId, String owner, String name) {
        Log.log(Level.INFO, this, "List all rooms");
        
        List<JsonNode> result = sites.listSites(nullEmpty(owner), nullEmpty(name));
                
        //we have to step through any results to remove the connectionDetails blocks.
        for(JsonNode j : result){
            JsonNode ownerNode = j.get("owner");
            if(ownerNode!=null && ownerNode.getNodeType().equals(JsonNodeType.STRING)){
                String ownerNodeString = ownerNode.textValue();
                //remove connectionDetailsBlocks for unauthenticated, or non matching ids, 
                //unless id is game-on.org
                if(authenticatedId==null || !(authenticatedId.equals(ownerNodeString))||authenticatedId.equals(GAME_ON_ORG)){
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
     * @param authenticatedId the person adding the new room
     * @param newRoom Room or Suite to add
     * @return Wired node containing the room or Suite
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site connectRoom(String authenticatedId, RoomInfo newRoom) {
        Log.log(Level.INFO, this, "Add new site: {0}", newRoom);

        // TODO: Input validation for connection details (most important)

        return sites.connectRoom(authenticatedId, newRoom);
    }

    /**
     * Get room by id
     *
     * @param authenticatedId person requesting the room, or null if unauthenticated
     * @param id Site/Room id
     * @return Complete information for the specified room/site
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site getRoom(String authenticatedId, String id) {
        Log.log(Level.INFO, this, "Lookup site: {0}", id);
        Site result = sites.getSite(id); 
        String owner = result.getOwner();
        if(authenticatedId==null || !(authenticatedId.equals(owner))||authenticatedId.equals(GAME_ON_ORG)){
            //unauthenticated, or non matching id, remove connection details block (except for game on id)
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
        Log.log(Level.INFO, this, "Update site: {0} {1}", id, roomInfo);

        return sites.updateRoom(authenticatedId, id, roomInfo);
    }


    /**
     * Delete site by id
     * @param authenticatedId person attempting the delete
     * @param id Site/Room id
     */
    public void deleteSite(String authenticatedId, String id) {
        Log.log(Level.INFO, this, "Delete site {0} by {1}", id, authenticatedId);

        sites.deleteSite(authenticatedId, id);
    }


    public ObjectMapper mapper() {
        return mapper;
    }
}
