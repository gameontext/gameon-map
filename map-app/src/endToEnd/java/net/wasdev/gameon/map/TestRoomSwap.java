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
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
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

//@Ignore
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
    public void testSwapRooms() throws JsonParseException, JsonMappingException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String owner = "TestOwner";
        String room1Name = "room1" + System.currentTimeMillis();
        String room2Name = "room2" + System.currentTimeMillis();;
        Site site1 = createRoom(owner, room1Name);
        Site site2 = createRoom(owner, room2Name);
        String room1Id = site1.getId();
        String room2Id = site2.getId();
        Coordinates site1Coords = site1.getCoord();
        Coordinates site2Coords = site2.getCoord();
        HttpResponse response = swapRooms(room1Id, room2Id);
        System.out.println("Swap rooms response has entity:");
        InputStream is = response.getEntity().getContent();
        byte [] ba = new byte[100];
        int read = 0;
        while((read = is.read(ba, 0, ba.length)) != -1) {
            System.out.print(new String(ba, 0, read));
        }
        is.close();
//        String jsonArray = response.readEntity(String.class);
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
    
    private HttpResponse swapRooms(String room1, String room2) throws ClientProtocolException, IOException, NoSuchAlgorithmException, InvalidKeyException {
//        Invocation.Builder invoBuild = createGameOnInvoBuilder("swapSites?room1Id=" + room1 + "&room2Id=" + room2);
        HttpClient client = HttpClientBuilder.create().build();
//        String roomName = "room1" + System.currentTimeMillis();
//        RoomInfo info = new RoomInfo();
//        info.setName(roomName);
//        info.setDescription("Boring description for " + roomName);
//        info.setFullName("Room for " + roomName);
//        info.setDoors(new Doors(roomName));
//
//        ConnectionDetails details = new ConnectionDetails();
//        details.setTarget("test-socket-for-"+roomName);
//        details.setType("test");
//        info.setConnectionDetails(details);
        String url = mapEndpoint + "swapSites?room1Id=" + room1 + "&room2Id=" + room2;
        System.out.println("Creating post request to url " + url);
        HttpPost request = new HttpPost(url);
//        HttpPost request = new HttpPost(mapEndpoint + "sites");
//        HttpGet request = new HttpGet(mapEndpoint + "sites/50942f91badba9fc1cb9dda64b0210d3");
        String userId = "sweep";
        String secret = "sweepSecret";
        HeaderAuthUtility utility = new HeaderAuthUtility(userId, secret);
        
        // Hash the body
        byte[] body = new byte[] {};
        String bodyHash = utility.buildHash(body);
        
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
        
//        request.setEntity((HttpEntity) info);
        
        HttpResponse response = client.execute(request);
//        Response response = invoBuild.accept(MediaType.APPLICATION_JSON_TYPE).post(null);
        System.out.println("SwapRooms returned:" + response.getStatusLine());
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
