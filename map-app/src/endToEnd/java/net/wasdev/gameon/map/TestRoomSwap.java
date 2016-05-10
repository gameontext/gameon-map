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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import net.wasdev.gameon.map.models.ConnectionDetails;
import net.wasdev.gameon.map.models.Coordinates;
import net.wasdev.gameon.map.models.Doors;
import net.wasdev.gameon.map.models.RoomInfo;
import net.wasdev.gameon.map.models.Site;

//@Ignore
public class TestRoomSwap {

    private static Collection<String> sitesToDelete;
    private String mapEndpoint = "http://127.0.0.1:9099/map/v1/";
    private String room1Id;
    private String room2Id;
    private Collection<String> roomsPreMove = new ArrayList<String>();
    private Coordinates room1CoordPreMove;
    private Coordinates room2CoordPreMove;
    
    @Before
    public void initialiseIdsToDeleteAndCreateRooms() throws JsonParseException, JsonMappingException, IOException {
        sitesToDelete = new HashSet<>();
        createRooms();
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
    public void testSwapRooms() throws JsonParseException, JsonMappingException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        HttpResponse response = swapRooms(room1Id, room2Id);
        
        System.out.println("Swap rooms response has entity:");
        InputStream is = response.getEntity().getContent();
        String entity = "";
        byte [] ba = new byte[100];
        int read = 0;
        while((read = is.read(ba, 0, ba.length)) != -1) {
            String output = new String(ba, 0, read);
            System.out.print(output);
            entity += output;
        }
        is.close();
        System.out.println("");
        
        ObjectMapper mapper = new ObjectMapper();
        Collection<Site> sites = mapper.readValue(entity, new TypeReference<Collection<Site>>(){});
        System.out.println("testSwapRooms returned sites " + sites);
        
        assertEquals(2, sites.size());
        
        Collection<String> roomsPostMove = new ArrayList<String>();
        Site site1PostMove = null;
        Site site2PostMove = null;
        Iterator<Site> itr = sites.iterator();
        while (itr.hasNext()) {
            Site site = itr.next();
            String siteId = site.getId();
            if (room1Id.equals(siteId)) {
                site1PostMove = site;
            }
            if (room2Id.equals(siteId)) {
                site2PostMove = site;
            }
            roomsPostMove.add(site.getId());
        }
        assertEquals("The api should pass back the rooms we passed in", roomsPreMove, roomsPostMove);
        
        Coordinates room1CoordPostMove = site1PostMove.getCoord();
        Coordinates room2CoordPostMove = site2PostMove.getCoord();
        
        assertEquals("Room 1 should now have the same coordinates as room 2 did before", room2CoordPreMove, room1CoordPostMove);
        assertEquals("Room 2 should now have the same coordinates as room 1 did before", room1CoordPreMove, room2CoordPostMove);
    }
    
    private void createRooms() throws JsonParseException, JsonMappingException, IOException {
        String owner = "TestOwner";
        String room1Name = "room1" + System.currentTimeMillis();
        String room2Name = "room2" + System.currentTimeMillis();;
        Site site1 = createRoom(owner, room1Name);
        Site site2 = createRoom(owner, room2Name);
        room1Id = site1.getId();
        room2Id = site2.getId();
        
        room1CoordPreMove = site1.getCoord();
        room2CoordPreMove = site2.getCoord();
        
        roomsPreMove.add(room1Id);
        roomsPreMove.add(room2Id);
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
    
    private HttpResponse swapRooms(String room1, String room2) throws ClientProtocolException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        HttpClient client = HttpClientBuilder.create().build();
        String url = mapEndpoint + "swapSites?room1Id=" + room1 + "&room2Id=" + room2;
        System.out.println("Creating post request to url " + url);
        HttpPost request = new HttpPost(url);
        String userId = "sweep";
        String secret = "sweepSecret";
        HeaderAuthUtility utility = new HeaderAuthUtility(userId, secret);
        
        //create the timestamp
        Instant now = Instant.now();
        String dateValue = now.toString();
        
        //create the signature
        String hmac = utility.buildHmac(Arrays.asList(new String[] {
                                   userId,
                                   dateValue,
                                   ""
                               }),secret);
        
        request.addHeader("gameon-id", userId);
        request.addHeader("gameon-date", dateValue);
        request.addHeader("gameon-sig-body", "");
        request.addHeader("gameon-signature", hmac);
        
        HttpResponse response = client.execute(request);
        System.out.println("SwapRooms returned:" + response.getStatusLine());
        return response;
    }
    
    private Invocation.Builder createGameOnInvoBuilder(String endpoint) {
        return createInvoBuilder(endpoint, "game-on.org", "fish");
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
