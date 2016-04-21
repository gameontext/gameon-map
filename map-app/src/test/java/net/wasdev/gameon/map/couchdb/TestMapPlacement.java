/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
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
package net.wasdev.gameon.map.couchdb;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.core.Response;

import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;

import net.wasdev.gameon.map.MapModificationException;
import net.wasdev.gameon.map.couchdb.auth.AccessOwnContentPolicy;
import net.wasdev.gameon.map.models.ConnectionDetails;
import net.wasdev.gameon.map.models.Coordinates;
import net.wasdev.gameon.map.models.Doors;
import net.wasdev.gameon.map.models.RoomInfo;
import net.wasdev.gameon.map.models.Site;

@Ignore
public class TestMapPlacement {

    protected static CouchDbInstance db;
    protected static MapRepository repo;
    protected static ObjectWriter debugWriter;
    private Collection<OwnerSitePair> sitesToDelete;

    @BeforeClass
    public static void beforeClass() throws MalformedURLException {
        HttpClient httpClient = new StdHttpClient.Builder()
                .url("http://192.168.99.100:5984/")
                .username("mapUser")
                .password("myCouchDBSecret")
                .build();
        db = new StdCouchDbInstance(httpClient);

        repo = new MapRepository();
        repo.db = db;
        repo.postConstruct();
        debugWriter = repo.sites.mapper.writerWithDefaultPrettyPrinter();
    }

    @Rule
    public TestName test = new TestName();

    @Before
    public void initialiseIdsToDelete() {
        sitesToDelete = new HashSet<>();
    }
    
    @After
    public void removeSitesCreatedForTest() {
        for (OwnerSitePair siteToDelete : sitesToDelete) {
            repo.deleteSite(siteToDelete.owner, siteToDelete.siteId);
        }
    }
    
    @Test
    public void testListRooms() throws JsonProcessingException {
        List<JsonNode> sites = repo.listSites(null, null, null);
        String fullString = debugWriter.writeValueAsString(sites);
        System.out.println(fullString);

        Assert.assertTrue("Coordinates should be contained in summary list", fullString.contains("\"coord\""));
        Assert.assertFalse("Exits should not be contained in summary list", fullString.contains("\"exits\""));

        // TODO: more things to tweak summary response
    }

    @Test
    public void testCreateUpdateRoom() throws JsonProcessingException {
        String owner = "test";
        String roomName = test.getMethodName() + System.currentTimeMillis();

        Site result = createRoom(owner, roomName);
        String fullString = debugWriter.writeValueAsString(result);

        // Make sure we get stuff for all of the exits back, and that the owner is not null
        Assert.assertNotNull("North exit should be described: " + fullString, result.getExits().getN());
        Assert.assertNotNull("South exit should be described: " + fullString, result.getExits().getS());
        Assert.assertNotNull("East exit should be described: " + fullString, result.getExits().getE());
        Assert.assertNotNull("West exit should be described: " + fullString, result.getExits().getW());
        Assert.assertEquals("Owner should be set: " + fullString,owner, result.getOwner());

        // List all the rooms ("" should be treated the same as null)
        List<JsonNode> after = repo.listSites(null, "", "");
        fullString = debugWriter.writeValueAsString(after);

        Assert.assertTrue("List should contain our new room: " + fullString, fullString.contains(result.getId()));
        Assert.assertFalse("List should not contain exits: " + fullString, fullString.contains("\"exits\""));

        // List rooms just for the "test" owner
        after = repo.listSites(new AccessOwnContentPolicy(owner), owner, null);
        fullString = debugWriter.writeValueAsString(after);

        Assert.assertTrue("List should contain our new room: " + fullString, fullString.contains(result.getId()));

        after = repo.sites.listSites(owner, result.getInfo().getName());
        Assert.assertEquals("List should only contain one element", 1, after.size());

        fullString = debugWriter.writeValueAsString(after);
        Assert.assertTrue("List should contain  ONLY our new room: " + fullString, fullString.contains(result.getId()));

        // UPDATE that room

        RoomInfo info = new RoomInfo();
        info.setName(roomName + "b");

        try {
            repo.updateRoom("shouldn't work", result.getId(), info);
            Assert.fail("Should not be able to update room with the wrong owner");
        } catch(MapModificationException mme) {
            Assert.assertEquals("Should return FORBIDDEN", Response.Status.FORBIDDEN, mme.getStatus());
        }

        Site update_result = repo.updateRoom(owner, result.getId(), info);
        fullString = debugWriter.writeValueAsString(update_result);

        Assert.assertEquals("Should see updated name: " + fullString, roomName + "b", update_result.getInfo().getName());
        Assert.assertEquals("Coordinates unchanged: " + fullString, result.getCoord(), update_result.getCoord());

        // Try to create another room with the same info (same owner + name)
        try {
            result = repo.connectRoom(owner, info);
            sitesToDelete.add(new OwnerSitePair(owner, result.getId()));
            Assert.fail("Should not be able to create a room with a not-unique owner-name combination");
        } catch(MapModificationException mme) {
            Assert.assertEquals("Should return CONFLICT", Response.Status.CONFLICT, mme.getStatus());
        }
    }

    @Test
    public void testFilteringByOwner() throws JsonProcessingException {
        /*
         * Create three rooms with different owners, we do three as the original
         * bug was due to a range of rooms having the wrong start and end index
         * so this should flush out all types of those errors.
         */
        String baseRoomName = test.getMethodName() + System.currentTimeMillis();
        String owner1 = "test1";
        String owner2 = "test2";
        String owner3 = "test3";
        Site room1 = createRoom(owner1, baseRoomName + "a");
        Site room2 = createRoom(owner2, baseRoomName + "b");
        Site room3 = createRoom(owner3, baseRoomName + "c");
        
        testSitesFilteredByOwner(owner1, room1, room2, room3);
        testSitesFilteredByOwner(owner2, room2, room1, room3);
        testSitesFilteredByOwner(owner3, room3, room1, room2);
    }
    
    private void testSitesFilteredByOwner(String owner, Site roomExpectedToBePresent, Site... roomsThatShouldBeMissing) throws JsonProcessingException {
        List<JsonNode> allSitesForOwner = repo.listSites(null, owner, null);
        String fullString = debugWriter.writeValueAsString(allSitesForOwner);
        Assert.assertTrue("The expected site " + roomExpectedToBePresent + " is missing from the list: " + fullString, fullString.contains(roomExpectedToBePresent.getId()));
        if (roomsThatShouldBeMissing != null) {
            for (Site unexpectedRoom : roomsThatShouldBeMissing) {
                Assert.assertFalse("The expected site " + unexpectedRoom + " should not be in the list: " + fullString, fullString.contains(unexpectedRoom.getId()));
            }
        }
    }
    
    @Test
    public void testFilterByName() throws JsonProcessingException {
        /*
         * There was a bug that the filter by name would return the named room
         * and previous one so create at least two rooms to show bug
         */
        String baseRoomName = test.getMethodName() + System.currentTimeMillis();
        String owner = "test";
        String room1Name = baseRoomName + "a";
        String room2Name = baseRoomName + "b";
        createRoom(owner, room1Name);
        createRoom(owner, room2Name);
        List<JsonNode> sitesForName1 = repo.listSites(null, null, room1Name);
        String fullString = debugWriter.writeValueAsString(sitesForName1);
        Assert.assertEquals("Only one site should have the name " + room1Name + " but got: " + fullString, 1, sitesForName1.size());
        List<JsonNode> sitesForName2 = repo.listSites(null, null, room2Name);
        fullString = debugWriter.writeValueAsString(sitesForName2);
        Assert.assertEquals("Only one site should have the name " + room2Name + " but got: " + fullString, 1, sitesForName2.size());
    }

    private Site createRoom(String owner, String roomName) {
        RoomInfo info = new RoomInfo();
        info.setName(roomName);
        info.setDescription("Boring description for " + roomName);
        info.setFullName("Room for " + roomName);
        info.setDoors(new Doors(roomName));

        ConnectionDetails details = new ConnectionDetails();
        details.setTarget("test-socket");
        details.setType("test");
        info.setConnectionDetails(details);

        // "connect" or place the new room into the map
        Site result = repo.connectRoom(owner, info);
        sitesToDelete.add(new OwnerSitePair(owner, result.getId()));
        return result;
    }

    @Test
    public void testDuplicateEmptyRoom() throws JsonProcessingException {
        List<Site> before = repo.sites.getEmptySites();

        // Grab an empty room
        Site emptySite = repo.sites.getEmptySite();
        System.out.println(debugWriter.writeValueAsString(emptySite));

        // Try adding another room with the same coordinates
        Site emptySite2 = repo.sites.createEmptySite(emptySite.getCoord());
        System.out.println(debugWriter.writeValueAsString(emptySite2));

        List<Site> after = repo.sites.getEmptySites();

        Assert.assertEquals("Original and result should have the same id", emptySite.getId(), emptySite2.getId());
        Assert.assertEquals("Original and result should have the same revision", emptySite.getRev(), emptySite2.getRev());
        Assert.assertEquals("Original and result should be for the same coordinates", emptySite.getCoord(), emptySite2.getCoord());

        Assert.assertEquals("There should be the same number of sites before and after we attempted to add one with existing coordinates",
                before.size(), after.size());
    }

    @Test
    public void testGetRoom() throws JsonProcessingException {
        // Grab an empty room
        Site emptySite = repo.sites.getEmptySite();

        Site result = repo.sites.getSite(emptySite.getId());

        Assert.assertEquals("Original and result should have the same id", emptySite.getId(), result.getId());
        Assert.assertEquals("Original and result should have the same revision", emptySite.getRev(), result.getRev());
        Assert.assertEquals("Original and result should be for the same coordinates", emptySite.getCoord(), result.getCoord());

        Assert.assertNull("Original fetch of empty site from the DB should not contain exits", emptySite.getExits());
        Assert.assertNotNull("Result should contain exits (even for empty room)", result.getExits());
    }


    @Test
    public void testSiteDelete() throws JsonProcessingException {
        String roomName = test.getMethodName() + "-1";

        Site testSite = new Site();
        testSite.setOwner("test");
        testSite.setId(roomName);
        testSite.setCoord(new Coordinates(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Create the document directly, way off in the corner.
        // We're skipping creation of surrounding empty links
        repo.sites.db.create(testSite);
        Assert.assertNotNull(testSite.getId());

        // RETRIEVE
        Site before = repo.sites.getSite(testSite.getId());
        Assert.assertEquals("Original and result should have the same id", testSite.getId(), before.getId());
        Assert.assertEquals("Original and result should have the same revision", testSite.getRev(), before.getRev());
        System.out.println(debugWriter.writeValueAsString(before));

        // DELETE
        String revision = repo.sites.deleteSite("test", testSite.getId());
        Assert.assertNotNull("Deleted revision should not be null", revision);

        try {
            repo.sites.getSite(testSite.getId());
            Assert.fail("Expected DocumentNotFoundException when requesting a deleted document");
        } catch(DocumentNotFoundException dne) {
        }

        List<Site> xy_replace = repo.sites.getByCoordinate(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Assert.assertEquals("A single replacement should be created for the same coordinates", 1, xy_replace.size());

        // attempt to delete the replacement
        repo.sites.db.delete(xy_replace.get(0));
    }
    
    private static class OwnerSitePair {
        private final String owner;
        private final String siteId;
        public OwnerSitePair(String owner, String siteId) {
            this.owner = owner;
            this.siteId = siteId;
        }
    }

}
