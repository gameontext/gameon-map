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

import java.util.ArrayList;
import java.util.Collection;
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
import org.gameontext.map.Log;
import org.gameontext.map.MapModificationException;
import org.gameontext.map.models.Coordinates;
import org.gameontext.map.models.Exit;
import org.gameontext.map.models.Exits;
import org.gameontext.map.models.RoomInfo;
import org.gameontext.map.models.Site;
import org.gameontext.map.models.SiteCoordinates;
import org.gameontext.map.models.SiteSwap;

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
 *   "exits" : { ... } // RETURN ONLY.
 * }
 * </pre>
 */
public class SiteDocuments {

    protected static final String DESIGN_DOC = "_design/site";

    protected final CouchDbConnector db;
    protected final ViewQuery allEmptySites;
    protected final ViewQuery all;

    protected final ObjectMapper mapper;

    protected SiteDocuments(CouchDbConnector db) {
        this.db = db;
        all = new ViewQuery().designDocId(DESIGN_DOC).viewName("all").cacheOk(true);
        allEmptySites = new ViewQuery().designDocId(DESIGN_DOC).viewName("empty_sites");

        mapper = new ObjectMapper();
    }

    /**
     * LIST
     * @param owner Owner of sites (optional)
     * @param name Name of site/room (optional)
     * @return List of all sites, possibly filtered by owner and/or name. Will not return null.
     */
    public List<JsonNode> listSites(String owner, String name) {

        List<JsonNode> sites = Collections.emptyList();

        ViewQuery viewQuery = createQueryToAppropriateView(owner, name);
        sites = db.queryView(viewQuery, JsonNode.class);

        if ( sites == null )
            return Collections.emptyList();

        return sites;
    }

    private ViewQuery createQueryToAppropriateView(String owner, String name) {
        if (owner == null && name == null) {
            return all;
        } else if (owner != null) {
            return createQueryToOwnerNameView(owner, name);
        } else {
            return createQueryToNameView(name);
        }
    }

    private ViewQuery createQueryToNameView(String name) {
        return createQueryWithoutKeys("name")
                .key(ComplexKey.of(name));
    }

    private ViewQuery createQueryToOwnerNameView(String owner, String name) {
        ViewQuery ownerNameQuery = createQueryWithoutKeys("owner_name");
        if (name == null) {
            ownerNameQuery = addKeyRangeForAllWithSameFirstEntryInComplexKey(ownerNameQuery, owner);
        } else {
            ownerNameQuery = addComplexKey(ownerNameQuery, owner, name);
        }
        return ownerNameQuery;
    }

    private ViewQuery addKeyRangeForAllWithSameFirstEntryInComplexKey(ViewQuery ownerNameQuery, String key) {
        return ownerNameQuery.startKey(ComplexKey.of(key))
                            .endKey(ComplexKey.of(key, ComplexKey.emptyObject()));
    }

    private ViewQuery addComplexKey(ViewQuery ownerNameQuery, String key1, String key2) {
        return ownerNameQuery.key(ComplexKey.of(key1, key2));
    }

    private ViewQuery createQueryWithoutKeys(String viewName) {
        return new ViewQuery()
                .designDocId(DESIGN_DOC)
                .viewName(viewName)
                .includeDocs(true);
    }


    /**
     * CREATE ROOM
     * @param owner Room/Suite owner
     * @param newRoom Room or Suite to add
     * @return Wired site containing the room or Suite
     * @throws MapModificationException
     */
    public Site connectRoom(String owner, RoomInfo newRoom) {
        Log.mapOperations(Level.FINE, this, "Add new room: {0}", newRoom);

        // TODO: Revisit this when we have groups/organizations.. *sigh*

        if ( owner == null ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room could not be created",
                    "Owner was not specified (unauthenticated)");
        }

        // Check for duplicate owner/room name: sloppy room registration
        // with no pre-check would land here potentially often.
        List<JsonNode> rooms = listSites(owner, newRoom.getName());
        if ( rooms.size() > 0 ) {
            throw new MapModificationException(Response.Status.CONFLICT,
                    "Unable to place room in the map",
                    "A room with this name ("+newRoom.getName()+") already exists for owner ("+owner+")");
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
     * @throws DocumentNotFoundException for unknown room
     */
    public Site getSite(String id) throws DocumentNotFoundException {
        Site site = getSiteWithoutExits(id);
        if ( site != null ) {
            Exits exits = getExits(site.getCoord());
            site.setExits(exits);
        }

        return site;
    }

    private Site getSiteWithoutExits(String id) throws DocumentNotFoundException{
        if (id == null || id.isEmpty()) {
            throw new MapModificationException(Response.Status.BAD_REQUEST,
                    "Site id must be set.",
                    "Site id passed in is " + id);
        }
        // get the document from the DB
        Site site = db.get(Site.class, id);
        return site;
    }

    /**
     * UDPATE ROOM
     * @param user Owner(?) of the room
     * @param id of room to update
     * @param roomInfo updated Room or Suite information
     * @return Wired site containing the room or Suite
     */
    public Site updateRoom(String user, String id, RoomInfo roomInfo) {
        Log.mapOperations(Level.FINE, this, "Update room: {0} {1}", id, roomInfo);

        // Get the site (includes reconstructing the exits)
        Site site = getSite(id);
        RoomInfo oldInfo = site.getInfo();

        // Revisit this with orgs.. *sigh*
        if ( site.getOwner() == null || !site.getOwner().equals(user) ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room " + id + " could not be updated",
                    user + " is not allowed to update room " + id);
        }

        site.setExits(null); // make sure exits is empty
        site.setInfo(roomInfo);
        db.update(site); // update DB

        // Room name change! check for duplicates..
        if ( !oldInfo.getName().equals(roomInfo.getName()) ) {
            List<JsonNode> rooms = listSites(user, roomInfo.getName());
            if ( rooms.size() > 1 ) {

                site.setInfo(oldInfo); // revert!
                db.update(site);

                throw new MapModificationException(Response.Status.CONFLICT,
                        "Room " + id + " could not be updated",
                        "A room with the modified name ("+roomInfo.getName()+") already exists for owner ("+user+")");
            }
        }

        // Find exits for return value
        Exits exits = getExits(site.getCoord());
        site.setExits(exits);
        return site;
    }

    /**
     * SWAP ROOMS
     * @param id1 First site in swap
     * @param id2 Second site in swap
     * @param room2Id
     */
    public Collection<Site> swapRooms(String id1, String id2) {
        Log.mapOperations(Level.FINE, this, "Swap rooms: {0} {1}", id1, id2);

        Site site1 = getSiteWithoutExits(id1);
        Site site2 = getSiteWithoutExits(id2);

        if (id1.equals(id2)) {
            throw new MapModificationException(Response.Status.BAD_REQUEST,
                    "Unable to swap rooms",
                    "Cannot swap a room with itself. Room id provided is " + id1);
        }
        site1.setExits(null);
        site2.setExits(null);
        Coordinates site1CoordsOld = site1.getCoord();
        Coordinates site2CoordsOld = site2.getCoord();
        site1.setCoord(site2CoordsOld);
        site2.setCoord(site1CoordsOld);
        Collection<Site> sites = new ArrayList<Site>();
        sites.add(site1);
        sites.add(site2);
        db.executeBulk(sites);
        return sites;
    }

    public List<Site> swapSites(SiteSwap siteSwap) {
        Log.mapOperations(Level.FINE, this, "Swap rooms: {0}", siteSwap);

        SiteCoordinates sc1 = siteSwap.getSite1();
        SiteCoordinates sc2 = siteSwap.getSite2();

        if (sc1.getId().equals(sc2.getId())) {
            throw new MapModificationException(Response.Status.BAD_REQUEST,
                    "Unable to swap sites",
                    "A site cannot be swapped with itself. Site id provided is " + sc1.getId());
        }

        Site site1 = getSiteWithoutExits(sc1.getId());
        Site site2 = getSiteWithoutExits(sc2.getId());

        if ( !sc1.getCoord().equals(site1.getCoord())
                || !sc2.getCoord().equals(site2.getCoord()) ) {
            throw new MapModificationException(Response.Status.CONFLICT,
                    "Unable to swap sites",
                    "One or both sites have moved");
        }

        site1.setCoord(sc2.getCoord());
        site2.setCoord(sc1.getCoord());

        List<Site> sites = new ArrayList<Site>();
        sites.add(site1);
        sites.add(site2);
        db.executeBulk(sites);
        return sites;
    }

    /**
     * DELETE
     *
     * @param user Owner(?) of the room
     * @param id of site to delete
     * @return the revision of the deleted document
     */
    public String deleteSite(String user, String id) throws DocumentNotFoundException {
        // Get the site first (need the coordinates)
        Site site = db.get(Site.class, id);

        // Revisit this with orgs.. *sigh*
        if ( site.getOwner() == null || !site.getOwner().equals(user) ) {
            throw new MapModificationException(Response.Status.FORBIDDEN,
                    "Room " + id + " could not be deleted",
                    user + " is not allowed to delete room " + id);
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
     * @param coord Position of the site in the map
     * @return Exits for the room located at (x,y) in the map
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
        Log.mapOperations(Level.FINEST, this, "Found neighbors: {0}", result);

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
     * the exit information.
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

        Log.mapOperations(Level.FINEST, this, "Added exit: {0} {1} {2}",
                mapper.valueToTree(targetSite.getCoord()),
                mapper.valueToTree(exit),
                mapper.valueToTree(exits));
    }

    protected Site assignEmptySite(String owner, RoomInfo newRoom) {

        // Get an unassigned empty site
        Site candidateSite = getEmptySite();
        Log.mapOperations(Level.FINEST, this, "Found empty node: {0}", candidateSite);

        candidateSite.setOwner(owner);
        candidateSite.setInfo(newRoom);
        candidateSite.setType("room");

        try {
            db.update(candidateSite);
        } catch (UpdateConflictException ex) {
            // If there is a conflict, we'll return null so that the caller tries again.
            // RETURN NULL: Caller should retry
            return null;
        }

        // Check again for duplicate owner/room name
        List<JsonNode> rooms = listSites(owner, newRoom.getName());
        if ( rooms.size() > 1 ) {
            // we have a duplicate. *le sigh*

            candidateSite.setOwner(null);
            candidateSite.setInfo(null);
            candidateSite.setType("empty");
            db.update(candidateSite);

            throw new MapModificationException(Response.Status.CONFLICT,
                    "Unable to place room in the map",
                    "A room with this name ("+newRoom.getName()+") already exists for owner ("+owner+")");
        }

        // Return the new/shiny site!
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
            assignExit(exits, "n", newSite);
        }
        if ( exits.getS() == null && newRoom.getY() > Integer.MIN_VALUE  ) {
            Site newSite = createEmptySite(newRoom.getX(), newRoom.getY()-1);
            assignExit(exits, "s", newSite);
        }
        if ( exits.getE() == null  && newRoom.getX() < Integer.MAX_VALUE ) {
            Site newSite = createEmptySite(newRoom.getX()+1, newRoom.getY());
            assignExit(exits, "e", newSite);
        }
        if ( exits.getW() == null  && newRoom.getX() > Integer.MIN_VALUE ) {
            Site newSite = createEmptySite(newRoom.getX()-1, newRoom.getY());
            assignExit(exits, "w", newSite);
        }
    }
}
