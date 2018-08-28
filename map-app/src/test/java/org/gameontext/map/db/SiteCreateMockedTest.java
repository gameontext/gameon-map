package org.gameontext.map.db;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewQuery;
import org.gameontext.map.MapModificationException;
import org.gameontext.map.model.RoomInfo;
import org.gameontext.map.model.Site;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class SiteCreateMockedTest {

    private static final String owner = "testOwner";

    @Mocked
    CouchDbConnector dbc;

    SiteDocuments docs;
    ObjectMapper mapper;
    ObjectWriter debugWriter;

    RoomInfo info;
    Site site;

    @Rule
    public TestName test = new TestName();

    @Before
    public void before() {
        System.out.println("\n====== " + test.getMethodName());

        mapper = new ObjectMapper();
        docs = new SiteDocuments(dbc, mapper);
        debugWriter = mapper.writerWithDefaultPrettyPrinter();

        info = new RoomInfo();
        info.setDescription("A room!");
        info.setFullName("BigRoom");
        info.setName("Name");

        site = new Site(-1, -1);
        site.setId("A");
    }

    @Test(expected=MapModificationException.class)
    public void testConnectRoomNameConflict() {

        List<Site> sites = Arrays.asList(site);

        new Expectations() {{
            dbc.queryView((ViewQuery) any, JsonNode.class); result = sites;
        }};

        docs.connectRoom(owner, info);
    }

    @Ignore
    public void testConnectRoomNoConflict() {

        List<Site> sites = Arrays.asList(site);

        new Expectations() {{
            dbc.queryView((ViewQuery) any); result = sites;
        }};

        docs.connectRoom(owner, info);
        System.out.println(site);

        new Verifications() {{
            dbc.update(site); times = 1;
            dbc.create(any); times = 4;
            assertNotNull( "createdOn should be set: " + site, site.getCreatedOn());
            assertNotNull( "assignedOn should be set: " + site, site.getAssignedOn());

        }};
    }

    @Test(expected=MapModificationException.class)
    public void testConnectRoomUpdateConflict() {

        List<Site> sites = Arrays.asList(site, site);

        new Expectations() {{
            dbc.queryView((ViewQuery) any, Site.class); result = sites;
            dbc.queryView((ViewQuery) any, JsonNode.class); returns(Collections.emptyList(), sites);
            dbc.update(site); result = new UpdateConflictException(); result = null;
        }};

        docs.connectRoom(owner, info);
        System.out.println(site);

        new Verifications() {{
            dbc.create(any); times = 4;
            dbc.update(site); times = 2;
            assertNotNull( "createdOn should be set: " + site, site.getCreatedOn());
            assertNotNull( "assignedOn should be set: " + site, site.getAssignedOn());
        }};
    }


}
