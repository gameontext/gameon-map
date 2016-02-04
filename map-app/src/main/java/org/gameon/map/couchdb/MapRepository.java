package org.gameon.map.couchdb;

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

@ApplicationScoped
public class MapRepository {

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
     * @param map
     * @return List of all sites, possibly filtered by owner and/or name. Will not return null.
     */
    public List<JsonNode> listSites(String owner, String name) {
        Log.log(Level.INFO, this, "List all rooms");

        return sites.listSites(nullEmpty(owner), nullEmpty(name));
    }

    private String nullEmpty(String parameter) {
        if ( parameter != null && parameter.trim().isEmpty() ) {
            return null;
        }
        return parameter;
    }

    /**
     * Connect/Add/Place a new room into the Map
     * @param owner
     * @param newRoom Room or Suite to add
     * @return Wired node containing the room or Suite
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site connectRoom(String owner, RoomInfo newRoom) {
        Log.log(Level.INFO, this, "Add new site: {0}", newRoom);

        // TODO: Input validation for connection details (most important)

        return sites.connectRoom(owner, newRoom);
    }

    /**
     * Get room by id
     *
     * @param id Site/Room id
     * @return Complete information for the specified room/site
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site getRoom(String id) {
        Log.log(Level.INFO, this, "Lookup site: {0}", id);

        return sites.getSite(id);
    }

    /**
     * Update room by id
     *
     * @param owner
     * @param id Site/Room id
     * @param updatedInfo Updated room information
     * @return Complete information for the specified room/site
     * @throws JsonProcessingException
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site updateRoom(String owner, String id, RoomInfo roomInfo) {
        Log.log(Level.INFO, this, "Update site: {0} {1}", id, roomInfo);

        return sites.updateRoom(owner, id, roomInfo);
    }


    /**
     * Delete site by id
     * @param owner Room owner (person attempting the delete)
     * @param id Site/Room id
     */
    public void deleteSite(String owner, String id) {
        Log.log(Level.INFO, this, "Delete site {0} by {1}", id, owner);

        sites.deleteSite(owner, id);
    }


    public ObjectMapper mapper() {
        return mapper;
    }
}
