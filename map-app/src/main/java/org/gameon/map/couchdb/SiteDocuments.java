package org.gameon.map.couchdb;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.Response;

import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.gameon.map.Log;
import org.gameon.map.MapModificationException;
import org.gameon.map.models.Coordinates;
import org.gameon.map.models.Exit;
import org.gameon.map.models.Exits;
import org.gameon.map.models.RoomInfo;
import org.gameon.map.models.Site;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Repository tracking and working with Sites (sites) in the map
 *
 * <pre>
 * {
 *   "_id": "1d1e2f39f95ee2ad88f40e67a5006748",
 *   "_rev": "2-959634980bb5cdc92b0c2cade7c70361",
 *   "coord": {
 *     "x": 0,
 *     "y": 0,
 *     "sort": 0
 *   },
 *   "info": {
 *     "name": "First Room",
 *     "fullName": "The First Room",
 *     "description": "A helpful room with doors in every possible direction.",
 *     "doors": {
 *       "n": "A knobbly wooden door with a rough carving or a friendly face",
 *       "w": "A fake wooden door with stickers of friendly faces plastered all over it",
 *       "s": "A warped wooden door with a friendly face branded on the corner",
 *       "e": "A polished wooden door with an inlaid friendly face",
 *       "u": "A scuffed and scratched oaken trap door",
 *       "d": "A rough-cut particle board hatch"
 *     },
 *     "connectionDetails": {
 *       "type": "websocket",
 *       "target": "wss://secondroom:9008/barn/ws"
 *     }
 *   },
 *   "type": "room",
 * }
 * </pre>
 */
public class SiteDocuments {

    static final String DESIGN_DOC = "_design/site";

    final CouchDbConnector db;
    final ViewQuery allEmptySites;
    final ViewQuery all;

    final ObjectMapper mapper;

    protected SiteDocuments(CouchDbConnector db) {
        this.db = db;
        all = new ViewQuery().designDocId(DESIGN_DOC).viewName("all").cacheOk(true);
        allEmptySites = new ViewQuery().designDocId(DESIGN_DOC).viewName("empty_sites");

        mapper = new ObjectMapper();
    }

    /**
     * LIST
     * @param map
     * @return
     */
    public List<JsonNode> listSites(String owner, String name) {

        List<JsonNode> sites = Collections.emptyList();

        if ( owner != null && name != null ) {
            ViewQuery owner_name = new ViewQuery().designDocId(DESIGN_DOC).viewName("owner_name")
                    .includeDocs(true)
                    .key(ComplexKey.of(owner, name));

            sites = db.queryView(owner_name, JsonNode.class);
        } else if ( owner != null ) {
            ViewQuery owner_name = new ViewQuery().designDocId(DESIGN_DOC).viewName("owner_name")
                    .includeDocs(true)
                    .startKey(owner)
                    .endKey(ComplexKey.of(owner, ComplexKey.emptyObject()));

            sites = db.queryView(owner_name, JsonNode.class);
        } else if ( name != null ) {
            ViewQuery name_only = new ViewQuery().designDocId(DESIGN_DOC).viewName("name")
                    .includeDocs(true)
                    .startKey(name)
                    .endKey(ComplexKey.of(name, ComplexKey.emptyObject()));

            sites = db.queryView(name_only, JsonNode.class);
        } else {
            sites = db.queryView(all, JsonNode.class);
        }

        return sites;
    }


    /**
     * CREATE ROOM
     * @param owner
     * @param newRoom Room or Suite to add
     * @return Wired site containing the room or Suite
     * @throws JsonProcessingException
     */
    public Site connectRoom(String owner, RoomInfo newRoom) {
        Log.log(Level.INFO, this, "Add new room: {0}", newRoom);

        // Revisit this with orgs.. *sigh*
        if ( owner == null ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room could not be created",
                    "Owner was not specified (unauthenticated)");
        }

        Site candidateSite = null;
        while (candidateSite == null ) {
            candidateSite = assignEmptySite(owner, newRoom);
        }

        // Yay! We have an allocated, previously-empty node
        // that has already been updated to point to this room.
        // Now we need to prep the response (with exits)..
        Exits exits = getExits(candidateSite.getCoord());

        // Make sure we have new empty sites (and add those to exits)
        createEmptyNeighbors(candidateSite.getCoord(), exits);

        // Set and return the response!
        candidateSite.setExits(exits);
        return candidateSite;
    }

    /**
     * RETRIEVE
     * Get site by id, fill in with related exit/neighbor information
     *
     * @param id Site/Room id
     * @return Complete information for the specified room/site
     */
    public Site getSite(String id) throws DocumentNotFoundException {
        // get the document from the DB
        Site site = db.get(Site.class, id);
        if ( site != null ) {
            Exits exits = getExits(site.getCoord());
            site.setExits(exits);
        }

        return site;
    }

    /**
     * UDPATE ROOM
     * @param owner Owner(?) of the room
     * @param id of room to update
     * @param roomInfo updated Room or Suite information
     * @return Wired site containing the room or Suite
     */
    public Site updateRoom(String owner, String id, RoomInfo roomInfo) {
        // Get the site (includes reconstructing the exits)
        Site site = getSite(id);

        // Revisit this with orgs.. *sigh*
        if ( site.getOwner() == null || !site.getOwner().equals(owner) ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room " + id + " could not be updated",
                    owner + " is not allowed to update room " + id);
        }

        site.setExits(null); // make sure exits is empty
        site.setInfo(roomInfo);
        db.update(site); // update DB

        // Find exits for return value
        Exits exits = getExits(site.getCoord());
        site.setExits(exits);
        return site;
    }


    /**
     * DELETE
     *
     * @param owner Owner(?) of the room
     * @param id of site to delete
     * @return the revision of the deleted document
     */
    public String deleteSite(String owner, String id) throws DocumentNotFoundException {
        System.out.println("HERE!!");
        // Get the site first (need the coordinates)
        Site site = db.get(Site.class, id);

        // Revisit this with orgs.. *sigh*
        if ( site.getOwner() == null || !site.getOwner().equals(owner) ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room " + id + " could not be deleted",
                    owner + " is not allowed to delete room " + id);
        }

        Coordinates coord = site.getCoord();
        String revision = db.delete(site);

        // Replace this site with an empty placeholder
        createEmptySite(coord);

        return revision;
    }

    /**
     * Find a given site by x,y coordinates
     *
     * @param x
     * @param y
     * @return List of sites located at x,y in the map (hoepfully only one!)
     */
    protected List<Site> getByCoordinate(int x, int y) {
        ViewQuery getByCoordinate = new ViewQuery()
                .designDocId(DESIGN_DOC)
                .viewName("uniqueSite")
                .reduce(false)
                .includeDocs(true)
                .key(ComplexKey.of(x, y));

        return db.queryView(getByCoordinate, Site.class);
    }


    /**
     * @param coord
     * @return
     * @throws JsonProcessingException
     */
    protected Exits getExits(Coordinates coord) {
        // Query for the neighbors of this node. Use "A" to "Z" to capture the
        // directional index (N/S/E/W/U/D), but skip this node (" "), as we have
        // that already.
        ViewQuery getNeighbors = new ViewQuery()
                .designDocId(DESIGN_DOC)
                .viewName("neighbors")
                .reduce(false) // do not reduce the result
                .includeDocs(true) // include referenced documents
                .startKey(ComplexKey.of(coord.getX(), coord.getY(), "A"))
                .endKey(ComplexKey.of(coord.getX(), coord.getY(), "Z"));

        ViewResult result = db.queryView(getNeighbors);
        Log.log(Level.INFO, this, "Found neighbors: {0}", result);

        Exits exits = new Exits();

        for(ViewResult.Row row : result.getRows() ) {
            JsonNode key = row.getKeyAsNode();
            String direction = key.get(2).asText(); // [0, 1, "n"]

            try {
                Site targetSite = mapper.treeToValue(row.getDocAsNode(), Site.class);
                assignExit(exits, direction, targetSite);
            } catch (JsonProcessingException e) {
                // Disagreement between our model class and what is in the data store :(
                Log.log(Level.SEVERE, this, "Unable to assign exit for {0} due to exception {1}", key, e);
                Log.log(Level.SEVERE, this, "Exception reading value from database", e);
            }
        }

        return exits;
    }

    /**
     * Assign information about a door from the targetSite into
     * the exit information. Note that orientation will flip here:
     * for the current room, the North Exit will be populated with
     * information about the South door of the adjacent/target room.
     *
     * @param exits Exits object to be populated with the new exit
     * @param key Direction of the exit
     * @param targetSite Target of the exit
     */
    protected void assignExit(Exits exits, String key, Site targetSite) {
        Exit exit = new Exit(targetSite, key);

        switch(key.toLowerCase()) {
            case "n" :
                exits.setN(exit);
                break;
            case "s" :
                exits.setS(exit);
                break;
            case "e" :
                exits.setE(exit);
                break;
            case "w" :
                exits.setW(exit);
                break;
        }

        Log.log(Level.FINEST, this, "Added exit: {0} {1} {2}",
                mapper.valueToTree(targetSite.getCoord()),
                mapper.valueToTree(exit),
                mapper.valueToTree(exits));
    }

    protected Site assignEmptySite(String owner, RoomInfo newRoom) {
        Site candidateSite = getEmptySite();
        Log.log(Level.INFO, this, "Found empty node: {0}", candidateSite);

        candidateSite.setOwner(owner);
        candidateSite.setInfo(newRoom);
        candidateSite.setType("room");

        // Save the changes to remove the node from the list of empty sites.
        // If there is a conflict, we'll return null so that the caller tries again.
        try {
            db.update(candidateSite);
        } catch (UpdateConflictException ex) {
            // RETURN NULL: Caller should retry
            return null;
        }

        // Return the new/shiny node!
        return candidateSite;
    }

    /**
     * @return a list of all empty sites
     */
    protected List<Site> getEmptySites() {
        return db.queryView(allEmptySites, Site.class);
    }

    /**
     * @return a single empty site
     */
    protected Site getEmptySite() {
        ViewQuery oneEmptySite = new ViewQuery()
                .designDocId(DESIGN_DOC)
                .viewName("empty_sites")
                .limit(1);

        List<Site> sites = db.queryView(oneEmptySite, Site.class);
        if ( sites.isEmpty() )
            return null;
        else
            return sites.get(0);
    }

    protected Site createEmptySite(Coordinates coord) {
        return createEmptySite(coord.getX(), coord.getY());
    }

    /**
     * Create an empty site for the specified x,y, coordinates
     *
     * @param x
     * @param y
     * @return Empty site
     */
    protected Site createEmptySite(int x, int y) {
        Site newSite = new Site(x, y);
        db.create(newSite);

        List<Site> emptySites = getByCoordinate(x, y);
        if ( emptySites.size() > 1 ) {
            // Duplicate.. remove the one we've added.
            db.delete(newSite);
            emptySites.remove(newSite);

            newSite = emptySites.get(0);
        } else {
            newSite.setType("empty");
            db.update(newSite);
        }

        return newSite;
    }

    /**
     * Create empty sites for every unbound side. Called only for rooms/suites,
     * this makes sure we always have empty rooms to choose from when adding new
     * rooms.
     *
     * @param newRoom X,Y coordinates for the new room
     * @param exits List of exits to be completed
     */
    protected void createEmptyNeighbors(Coordinates newRoom, Exits exits) {
        // Protect against MAX/MIN values so we can use the corners for testing.

        if ( exits.getN() == null && newRoom.getY() < Integer.MAX_VALUE ) {
            Site newSite = createEmptySite(newRoom.getX(), newRoom.getY()+1);
            assignExit(exits, "N", newSite);
        }
        if ( exits.getS() == null && newRoom.getY() > Integer.MIN_VALUE  ) {
            Site newSite = createEmptySite(newRoom.getX(), newRoom.getY()-1);
            assignExit(exits, "S", newSite);
        }
        if ( exits.getE() == null  && newRoom.getX() < Integer.MAX_VALUE ) {
            Site newSite = createEmptySite(newRoom.getX()+1, newRoom.getY());
            assignExit(exits, "E", newSite);
        }
        if ( exits.getW() == null  && newRoom.getX() > Integer.MIN_VALUE ) {
            Site newSite = createEmptySite(newRoom.getX()-1, newRoom.getY());
            assignExit(exits, "W", newSite);
        }
    }

}
