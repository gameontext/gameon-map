package org.gameontext.map;

import static org.junit.Assert.assertTrue;

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

import org.gameontext.map.model.ConnectionDetails;
import org.gameontext.map.model.Doors;
import org.gameontext.map.model.RoomInfo;
import org.gameontext.map.model.Site;
import org.gameontext.signed.SignedClientRequestFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class TestRoomCreate {

    private static Collection<String> sitesToDelete;
    String mapEndpoint = "http://127.0.0.1:9099/map/v1/";

    @Before
    public void initialiseIdsToDelete() {
        sitesToDelete = new HashSet<>();
    }

    @After
    public void removeSitesCreatedForTest() {
        for (String siteToDelete : sitesToDelete) {
            Invocation.Builder invoBuild = createInvoBuilder("sites/" + siteToDelete);
            //TODO: This doesn't work because game-on doesn't have access to delete rooms!
            Response response = invoBuild.accept(MediaType.APPLICATION_JSON_TYPE).delete();
            System.out.println("Request to delete site with id " + siteToDelete + " returned with status " + response.getStatus());
        }
    }

    @Test
    public void testcreateRooms() throws JsonParseException, JsonMappingException, IOException {
        String owner = "TestOwner";
        String roomName = "room" + System.currentTimeMillis();
        String roomId = createRoom(owner, roomName);
        String rooms = getRooms();
        assertTrue("Room with id " + roomId + " should have been created.", rooms.contains(roomId));
    }

    private String createRoom(String owner, String roomName) throws JsonParseException, JsonMappingException, IOException {
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
        Invocation.Builder invoBuild = createInvoBuilder("sites");

        Response response = invoBuild.accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.json((Object) info));

        String siteString = response.readEntity(String.class);
        System.out.println("Create room returned site string:" + siteString);

        ObjectMapper mapper = new ObjectMapper();
        Site site = mapper.readValue(siteString, Site.class);
        System.out.println("Created site " + site);
        sitesToDelete.add(site.getId());
        return site.getId();
    }

    private String getRooms() {
        Invocation.Builder invoBuild = createInvoBuilder("sites");
        Response response = invoBuild.accept(MediaType.APPLICATION_JSON_TYPE).get();

        String siteListString = response.readEntity(String.class);
        System.out.println("GetRooms found site list string:" + siteListString);

        return siteListString;
    }

    private Invocation.Builder createInvoBuilder(String endpoint) {
        String url = mapEndpoint + endpoint;
        System.out.println("Creating invocation for url " + url);

        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        SignedClientRequestFilter filter = new SignedClientRequestFilter("gameontext.org", "fish");
        client.register(filter);

        WebTarget target = client.target(url);
        return target.request();
    }

}
