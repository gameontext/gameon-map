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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.BiPredicate;

import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.fasterxml.jackson.databind.ObjectWriter;

import net.wasdev.gameon.map.MapModificationException;
import net.wasdev.gameon.map.couchdb.auth.AccessCertainResourcesPolicy;
import net.wasdev.gameon.map.couchdb.auth.NoAccessPolicy;
import net.wasdev.gameon.map.models.ConnectionDetails;
import net.wasdev.gameon.map.models.Coordinates;
import net.wasdev.gameon.map.models.Doors;
import net.wasdev.gameon.map.models.Exits;
import net.wasdev.gameon.map.models.RoomInfo;
import net.wasdev.gameon.map.models.Site;

@Ignore
public class TestRoomSwap {

    public class RoomIdPair {

        private final String room1Id;
        private final String room2Id;
        
        public RoomIdPair(String room1Id, String room2Id) {
            this.room1Id = room1Id;
            this.room2Id = room2Id;
        }

        public String getRoom1Id() {
            return room1Id;
        }

        public String getRoom2Id() {
            return room2Id;
        }

    }

    private static final int ROOM_COUNT = 50;

    protected static CouchDbInstance db;
    protected static MapRepository repo;
    protected static ObjectWriter debugWriter;
    private static Collection<OwnerSitePair> sitesToDelete;
    private static AccessCertainResourcesPolicy swapRoomsAccessPolicy;
    private static String roomOwner;

    @BeforeClass
    public static void beforeClass() throws MalformedURLException {
        roomOwner = "testOwner";
        HttpClient httpClient = new StdHttpClient.Builder()
                .url("http://127.0.0.1:5984/")
                .build();
        db = new StdCouchDbInstance(httpClient);

        repo = new MapRepository();
        repo.db = db;
        repo.postConstruct();
        debugWriter = repo.sites.mapper.writerWithDefaultPrettyPrinter();
        swapRoomsAccessPolicy = new AccessCertainResourcesPolicy(Collections.singleton(SiteSwapper.class));
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
    public void testSwapRooms() {
        RoomIdPair pair = generateMap((Coordinates room1Coords, Coordinates coords) -> {
            int xDiff = Math.abs(coords.getX() - room1Coords.getX());
            int yDiff = Math.abs(coords.getY() - room1Coords.getY());
            return xDiff > 1 || yDiff < -1;
        });

        String room1Id = pair.getRoom1Id();
        String room2Id = pair.getRoom2Id();
        
        Coordinates room1CoordPreMove = getCoordinatesForRoomId(room1Id);
        Coordinates room2CoordPreMove = getCoordinatesForRoomId(room2Id);
        assertFalse("The rooms should not have the same coordinates", room1CoordPreMove.equals(room2CoordPreMove));
        
        Site room1 = repo.getRoom(new NoAccessPolicy(), room1Id);
        System.out.println("-----------------------------\nAfter allRooms:\n"+room1);
        
        repo.swapRooms(swapRoomsAccessPolicy, roomOwner, room1Id, room2Id);
        
        
        room1 = repo.getRoom(new NoAccessPolicy(), room1Id);
        System.out.println("-----------------------------\nPost allRooms:\n"+room1);
        
        Coordinates room1CoordPostMove = getCoordinatesForRoomId(room1Id);
        Coordinates room2CoordPostMove = getCoordinatesForRoomId(room2Id);
        
        assertTrue("Room1 should now be where room2 was.", room1CoordPostMove.equals(room2CoordPreMove));
        assertTrue("Room2 should now be where room1 was.", room2CoordPostMove.equals(room1CoordPreMove));
    }

    @Test
    public void testSwapDistantRoomsExits() {
        RoomIdPair pair = generateMap((Coordinates room1Coords, Coordinates coords) -> {
            int xDiff = Math.abs(coords.getX() - room1Coords.getX());
            int yDiff = Math.abs(coords.getY() - room1Coords.getY());
            return xDiff > 1 || yDiff < -1;
        });

        String room1Id = pair.getRoom1Id();
        String room2Id = pair.getRoom2Id();
        
        Exits room1ExitsPreMove = getExitsForRoomId(room1Id);
        Exits room2ExitsPreMove = getExitsForRoomId(room2Id);
        
        repo.swapRooms(swapRoomsAccessPolicy, null, room1Id, room2Id);
        
        Exits room1ExitsPostMove = getExitsForRoomId(room1Id);
        Exits room2ExitsPostMove = getExitsForRoomId(room2Id);
        
        assertEquals("The exits on Room 1 should be what was previously the exits on Room 2", room2ExitsPreMove, room1ExitsPostMove);
        assertEquals("The exits on Room 2 should be what was previously the exits on Room 1", room1ExitsPreMove, room2ExitsPostMove);
    }
    
    @Test
    public void testSwapAdjacentRoomsExits() {
        String owner = "test";
        Site room1Site = createRoom(owner, test.getMethodName() + "room1");
        Coordinates room1Coords = room1Site.getCoord();
        Site room2Site = null;
        int creationCount = 0;
        Coordinates roomToTheEastCoords = new Coordinates(room1Coords.getX() + 1, room1Coords.getY());
        while (room2Site == null && creationCount <= 8) {
            String roomName = test.getMethodName() + creationCount;
            Site site = createRoom(owner, roomName);
            Coordinates coord = site.getCoord();
            creationCount++;
            if (coord.equals(roomToTheEastCoords)) {
                room2Site = site;
            }
        }

        assertNotNull("Should have found a room that is directly to the East of room 1.", room2Site);

        String room1Id = room1Site.getId();
        String room2Id = room2Site.getId();
        Exits room1ExitsPreMove = room1Site.getExits();
        System.out.println("Room 1 Exits PreMove are " + room1ExitsPreMove);
        Exits room2ExitsPreMove = room2Site.getExits();
        System.out.println("Room 2 Exits PreMove are " + room2ExitsPreMove);
        repo.swapRooms(swapRoomsAccessPolicy, null, room1Id, room2Id);
        Site room1PostMove = repo.getRoom(new NoAccessPolicy(), room1Id);
        Site room2PostMove = repo.getRoom(new NoAccessPolicy(), room2Id);
        Exits room1ExitsPostMove = room1PostMove.getExits();
        System.out.println("Room 1 Exits PostMove are " + room1ExitsPostMove);
        Exits room2ExitsPostMove = room2PostMove.getExits();
        System.out.println("Room 2 Exits PostMove are " + room2ExitsPostMove);

    }
    
    @Test
    public void testCorrectRoomsReturned() {
        RoomIdPair pair = generateMap((Coordinates room1Coords, Coordinates coords) -> {
            int xDiff = Math.abs(coords.getX() - room1Coords.getX());
            int yDiff = Math.abs(coords.getY() - room1Coords.getY());
            return xDiff > 1 || yDiff < -1;
        });

        String room1Id = pair.getRoom1Id();
        String room2Id = pair.getRoom2Id();
        
        Collection<String> roomsPreMove = new ArrayList<String>();
        roomsPreMove.add(room1Id);
        roomsPreMove.add(room2Id);
        
        
        Collection<Site> sitesReturned = repo.swapRooms(swapRoomsAccessPolicy, null, room1Id, room2Id);
        assertEquals(sitesReturned.size(), 2);
        
        Collection<String> roomsPostMove = new ArrayList<String>();
        Iterator<Site> itr = sitesReturned.iterator();
        while (itr.hasNext()) {
            roomsPostMove.add(itr.next().getId());
        }
        assertEquals(roomsPreMove, roomsPostMove);
        
    }
    
    @Test
    public void testSwapRoomsNullAuth() {
        RoomIdPair pair = generateMap((Coordinates room1Coords, Coordinates coords) -> {
            int xDiff = Math.abs(coords.getX() - room1Coords.getX());
            int yDiff = Math.abs(coords.getY() - room1Coords.getY());
            return xDiff > 1 || yDiff < -1;
        });

        String room1Id = pair.getRoom1Id();
        String room2Id = pair.getRoom2Id();
        
        MapModificationException e = null;
        try {
            repo.swapRooms(null, roomOwner, room1Id, room2Id);
        } catch (MapModificationException ex){
            e = ex;
        }
        assertNotNull(e);
        String expectedMessage = "User " + roomOwner + " does not have permission to swap rooms.";
        String actualMessage = e.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }
    
    @Test
    public void testSwapRoomsInvalidAuth() {
        RoomIdPair pair = generateMap((Coordinates room1Coords, Coordinates coords) -> {
            int xDiff = Math.abs(coords.getX() - room1Coords.getX());
            int yDiff = Math.abs(coords.getY() - room1Coords.getY());
            return xDiff > 1 || yDiff < -1;
        });

        String room1Id = pair.getRoom1Id();
        String room2Id = pair.getRoom2Id();
        
        MapModificationException e = null;
        try {
            repo.swapRooms(new NoAccessPolicy(), roomOwner, room1Id, room2Id);
        } catch (MapModificationException ex){
            e = ex;
        }
        assertNotNull("Swap request should result in MapModificationException.", e);
        String expectedMessage = "User " + roomOwner + " does not have permission to swap rooms.";
        String actualMessage = e.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }
    
    @Test
    public void testSwapRoomWithItself() {
        String owner = "test";
        String roomName = test.getMethodName() + "room1";

        Site roomSite = createRoom(owner, roomName);

        String roomId = roomSite.getId();
        
        MapModificationException e = null;
        try {
            repo.swapRooms(swapRoomsAccessPolicy, roomOwner, roomId, roomId);
        } catch (MapModificationException ex){
            e = ex;
        }
        assertNotNull("Swap request should result in MapModificationException.", e);
        String expectedMessage = "Cannot swap a room with itself.";
        String actualMessage = e.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }
    
    @Test
    public void testSwapRoomsNullRoom() {
        MapModificationException e = null;
        try {
            repo.swapRooms(swapRoomsAccessPolicy, roomOwner, null, "roomId");
        } catch (MapModificationException ex) {
            e = ex;
        }
        assertNotNull("Swap request should result in MapModificationException.", e);
        String expectedMessage = "Site id must be set.";
        String actualMessage = e.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }
    
    @Test
    public void testSwapRoomsUnsetRoom() {
        MapModificationException e = null;
        try {
            repo.swapRooms(swapRoomsAccessPolicy, roomOwner, "", "roomId");
        } catch (MapModificationException ex) {
            e = ex;
        }
        assertNotNull("Swap request should result in MapModificationException.", e);
        String expectedMessage = "Site id must be set.";
        String actualMessage = e.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }
    
    @Test
    public void testSwapRoomsNonExistentRoom() {
        DocumentNotFoundException e = null;
        try {
            repo.swapRooms(swapRoomsAccessPolicy, roomOwner, "roomId1", "roomId2");
        } catch (DocumentNotFoundException ex) {
            e = ex;
        }
        assertNotNull("Swap request should result in DocumentNotFoundException.", e);
    }

    private Coordinates getCoordinatesForRoomId(String roomId) {
        Site room = repo.getRoom(new NoAccessPolicy(), roomId);
        assertNotNull("Should have found room " + roomId, room);
        Coordinates coords = room.getCoord();
        assertNotNull("The room " + roomId + " should have a Coordinates object", coords);
        return coords;
    }


    private Exits getExitsForRoomId(String roomId) {
        Site room = repo.getRoom(new NoAccessPolicy(), roomId);
        assertNotNull("Should have found room " + roomId, room);
        Exits exits = room.getExits();
        assertNotNull("The room " + roomId + " should have an Exits object", exits);
        return exits;
    }
    
    private RoomIdPair generateMap(BiPredicate<Coordinates, Coordinates> compareStrategy) {
        String owner = "test";
        String room1Name = test.getMethodName() + "room1";

        Site room1Site = createRoom(owner, room1Name);
        System.out.println("Room 1 is " + room1Site);
        String room1Id = room1Site.getId();

        String room2Id = null;
        Coordinates room1Coords = room1Site.getCoord();
        int creationCount = 0;
        while (room2Id == null && creationCount <= 8) {
            String roomName = test.getMethodName() + creationCount;
            Site site = createRoom(owner, roomName);
            Coordinates coord = site.getCoord();
            creationCount++;
            if (compareStrategy.test(room1Coords, coord)) {
                room2Id = site.getId();
            }
        }
        assertNotNull("Should have found a room that is not next to Room 1", room2Id);
        RoomIdPair roomIdPair = new RoomIdPair(room1Id, room2Id);
        return roomIdPair;
    }

    private static Site createRoom(String owner, String roomName) {
        RoomInfo info = new RoomInfo();
        info.setName(roomName);
        info.setDescription("Boring description for " + roomName);
        info.setFullName("Room for " + roomName);
        info.setDoors(new Doors(roomName));

        ConnectionDetails details = new ConnectionDetails();
        details.setTarget("test-socket-for-"+roomName);
        details.setType("test");
        info.setConnectionDetails(details);

        // "connect" or place the new room into the map
        Site result = repo.connectRoom(owner, info);
        sitesToDelete.add(new OwnerSitePair(owner, result.getId()));
        return result;
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
