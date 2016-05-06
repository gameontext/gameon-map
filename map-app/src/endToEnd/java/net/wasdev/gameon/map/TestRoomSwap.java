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
package net.wasdev.gameon.map;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import net.wasdev.gameon.map.models.ConnectionDetails;
import net.wasdev.gameon.map.models.Coordinates;
import net.wasdev.gameon.map.models.Doors;
import net.wasdev.gameon.map.models.RoomInfo;
import net.wasdev.gameon.map.models.Site;

@Ignore
public class TestRoomSwap {

    private static Collection<String> sitesToDelete;
    String mapEndpoint = "http://127.0.0.1:9099/map/v1/";
    
    @Before
    public void initialiseIdsToDelete() {
        sitesToDelete = new HashSet<>();
    }

    @After
    public void removeSitesCreatedForTest() {
        for (String siteToDelete : sitesToDelete) {
            Invocation.Builder invoBuild = createGameOnInvoBuilder("sites/" + siteToDelete);
            //TODO: This doesn't work because game-on.org cannot delete rooms.
            // Need to create a mechanism for a user to register and get their API Key
            Response response = invoBuild.accept(MediaType.APPLICATION_JSON_TYPE).delete();
            System.out.println("Request to delete site with id " + siteToDelete + " returned with status " + response.getStatus());
        }
    }
    
    @Test
    public void testSwapRooms() throws JsonParseException, JsonMappingException, IOException {
        String owner = "TestOwner";
        String room1Name = "room1" + System.currentTimeMillis();
        String room2Name = "room2" + System.currentTimeMillis();;
        Site site1 = createRoom(owner, room1Name);
        Site site2 = createRoom(owner, room2Name);
        String room1Id = site1.getId();
        String room2Id = site2.getId();
        Coordinates site1Coords = site1.getCoord();
        Coordinates site2Coords = site2.getCoord();
        Response response = swapRooms(room1Id, room2Id);
        String jsonArray = response.readEntity(String.class);
        System.out.println("Swap rooms response has entity:" + jsonArray);
        //TODO: the message returned was: Swap rooms response has entity:Error 403: Request made to unknown url pattern. /map/v1/swapSites
    }
    
    private Site createRoom(String owner, String roomName) throws JsonParseException, JsonMappingException, IOException {
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
        Invocation.Builder invoBuild = createGameOnInvoBuilder("sites");
        
        Response response = invoBuild.accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.json((Object) info));
        
        String siteString = response.readEntity(String.class);
        System.out.println("Create room returned site string:" + siteString);
        
        ObjectMapper mapper = new ObjectMapper();
        Site site = mapper.readValue(siteString, Site.class);
        System.out.println("Created site " + site);
        sitesToDelete.add(site.getId());
        return site;
    }
    
    private Response swapRooms(String room1, String room2) {
        Invocation.Builder invoBuild = createSweepInvoBuilder("swapSites?room1Id=" + room1 + "&room2Id=" + room2);
        Response response = invoBuild.accept(MediaType.APPLICATION_JSON_TYPE).post(null);
        System.out.println("SwapRooms returned:" + response);
        return response;
    }
    
    private Invocation.Builder createGameOnInvoBuilder(String endpoint) {
        return createInvoBuilder(endpoint, "game-on.org", "fish");
    }
    
    private Invocation.Builder createSweepInvoBuilder(String endpoint) {
        return createInvoBuilder(endpoint, "sweep", "sweepSecret");
    }
    
    private Invocation.Builder createInvoBuilder(String endpoint, String user, String password) {
        String url = mapEndpoint + endpoint;
        System.out.println("Creating invocation for url " + url);
        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        client.register(new HeaderAuthInterceptorUtility(user, password));
        WebTarget target = client.target(url);
        return target.request();
    }

}
