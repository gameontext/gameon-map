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
import org.gameon.map.models.RoomInfo;
import org.gameon.map.models.Site;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

@ApplicationScoped
public class MapRepository {

    @Resource(name = "couchdb/connector")
    protected CouchDbInstance db;

    protected SiteDocuments sites;

    @PostConstruct
    protected void postConstruct() {
        String dbname = "map_repository";

        try {
            // Connect to the database with the specified
            CouchDbConnector dbc = db.createConnector(dbname, false);
            Log.log(Level.INFO, this, "Connected to {0}", dbname);

            // Ensure required views exist
            sites = new SiteDocuments(dbc);
        } catch (Exception e) {
            // Log the warning, and then re-throw to prevent this class from going into service,
            // which will prevent injection to the Health check, which will make the app stay down.
            Log.log(Level.WARNING, this, "Unable to connect to database", e);
            throw e;
        }
    }


    /**
     * List of all not-empty rooms
     * @return list of rooms (all)
     */
    public List<JsonNode> listSites() {
        Log.log(Level.INFO, this, "List all rooms");

        return sites.listSites();
    }

    /**
     * Connect/Add/Place a new room into the Map
     *
     * @param newRoom Room or Suite to add
     * @return Wired node containing the room or Suite
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site connectRoom(RoomInfo newRoom) {
        Log.log(Level.INFO, this, "Add new site: {0}", newRoom);

        // TODO: Input validation for connection details (most important)

        return sites.connectRoom(newRoom);
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
     * @param id Site/Room id
     * @param updatedInfo Updated room information
     * @return Complete information for the specified room/site
     * @throws JsonProcessingException
     * @throws MapModificationException if something goes awry creating the room
     */
    public Site updateRoom(String id, RoomInfo roomInfo) {
        Log.log(Level.INFO, this, "Update site: {0} {1}", id, roomInfo);

        return sites.updateRoom(id, roomInfo);
    }


    /**
     * Delete site by id
     * @param id Site/Room id
     */
    public void deleteSite(String id) {
        Log.log(Level.INFO, this, "Delete site: {0}", id);
        sites.deleteSite(id);
    }



}
