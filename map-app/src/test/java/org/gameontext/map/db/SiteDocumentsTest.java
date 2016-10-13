package org.gameontext.map.db;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.gameontext.map.model.Coordinates;
import org.gameontext.map.model.Exit;
import org.gameontext.map.model.Exits;
import org.gameontext.map.model.RoomInfo;
import org.gameontext.map.model.Site;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import mockit.Expectations;
import mockit.Mocked;

public class SiteDocumentsTest {

    @Mocked
    CouchDbConnector dbc;

    SiteDocuments docs;

    RoomInfo info;
    Site site;

    @Rule
    public TestName test = new TestName();

    @Before
    public void before() {
        System.out.println("\n====== " + test.getMethodName());

        docs = new SiteDocuments(dbc);

        info = new RoomInfo();
        info.setDescription("A room!");
        info.setFullName("BigRoom");
        info.setName("Name");

        site = new Site(-1, -1);
        site.setId("A");
        site.setInfo(info);
    }

    @Test
    public void testGetExits(@Mocked ViewResult queryResult, @Mocked ViewResult.Row row, @Mocked JsonNode key) throws Exception {

        List<ViewResult.Row> rows = Arrays.asList(row);
        Exits exits = new Exits();
        exits.setN(new Exit(site, "n"));
        site.setExits(exits);

        final ObjectMapper anyInstance = new ObjectMapper();

        new Expectations(ObjectMapper.class) {{
            anyInstance.treeToValue((TreeNode) any, Site.class); returns(site);
        }};

        new Expectations() {{
            dbc.queryView((ViewQuery) any); returns(queryResult);
            queryResult.getRows(); returns(rows);
            row.getKeyAsNode(); returns(key);
            key.get(2); returns(JsonNodeFactory.instance.textNode("n"));
         }};

        Exits e = docs.getExits(new Coordinates(1,2));
        Assert.assertEquals("Site should be bound to the north", site.getId(), e.getN().getId());
    }

    @Test
    public void testUnusedCoordinates(@Mocked ViewResult queryResult, @Mocked ViewResult.Row row, @Mocked JsonNode key) throws Exception {
        Exits exits = new Exits();
        site.setExits(exits);

        final ObjectMapper anyInstance = new ObjectMapper();

        new Expectations(ObjectMapper.class) {{
            anyInstance.treeToValue((TreeNode) any, Site.class); returns(site);
        }};

        docs.assignExit(exits, "n", site);
        new Expectations() {{
            dbc.queryView((ViewQuery) any, Site.class); returns(Arrays.asList(site, site), Collections.emptyList());
            dbc.queryView((ViewQuery) any); returns(queryResult);
            queryResult.getRows(); returns(Collections.emptyList());
        }};

        Coordinates c0 = new Coordinates(5,-5);
        Coordinates c1 = docs.findUnusedCoordinate(c0);
        System.out.println(c0 + " --> " + c1);
        Assert.assertNotEquals("Coordinates should be different", c0, c1);
        Assert.assertEquals("No exit present, move north", c0.getX(), c1.getX());
        Assert.assertEquals("No exit present, move north", c0.getY() + 1, c1.getY());


        site.setCoord(c1);
        docs.assignExit(exits, "n", site);
        new Expectations() {{
            dbc.queryView((ViewQuery) any, Site.class); returns(Arrays.asList(site, site), Collections.emptyList());
            queryResult.getRows(); returns(Arrays.asList(row, row));
            key.get(2); returns(JsonNodeFactory.instance.textNode("n"));
        }};
        Coordinates c2 = docs.findUnusedCoordinate(c1);
        System.out.println(c1 + " --> " + c2);
        Assert.assertNotEquals("Coordinates should be different", c1, c2);
        Assert.assertEquals("North exits is present, move south", c1.getX(), c2.getX());
        Assert.assertEquals("North exits is present, move south", c1.getY() - 1, c2.getY());

        site.setCoord(c2);
        docs.assignExit(exits, "s", site);
        new Expectations() {{
            dbc.queryView((ViewQuery) any, Site.class); returns(Arrays.asList(site, site), Collections.emptyList());
            queryResult.getRows(); returns(Arrays.asList(row, row));
            key.get(2); returns(JsonNodeFactory.instance.textNode("n"), JsonNodeFactory.instance.textNode("s"));
        }};
        Coordinates c3 = docs.findUnusedCoordinate(c2);
        System.out.println(c2 + " --> " + c3);
        Assert.assertNotEquals("Coordinates should be different", c2, c3);
        Assert.assertEquals("North and South exits are present, move east", c2.getY(), c3.getY());
        Assert.assertEquals("North and South exits are present, move east", c2.getX() + 1, c3.getX());

        site.setCoord(c3);
        docs.assignExit(exits, "e", site);
        new Expectations() {{
            dbc.queryView((ViewQuery) any, Site.class); returns(Arrays.asList(site, site), Collections.emptyList());
            queryResult.getRows(); returns(Arrays.asList(row, row, row));
            key.get(2); returns(JsonNodeFactory.instance.textNode("n"), JsonNodeFactory.instance.textNode("s"), JsonNodeFactory.instance.textNode("e"));
        }};
        Coordinates c4 = docs.findUnusedCoordinate(c3);
        System.out.println(c3 + " --> " + c4);
        Assert.assertNotEquals("Coordinates should be different", c3, c4);
        Assert.assertEquals("North, South, and East exits are present, move west", c3.getY(), c4.getY());
        Assert.assertEquals("North, South, and East exits are present, move west", c3.getX() - 1, c4.getX());

        // Diagnoal slide...
        site.setCoord(c4);
        docs.assignExit(exits, "w", site);
        new Expectations() {{
            dbc.queryView((ViewQuery) any, Site.class); returns(Arrays.asList(site, site), Collections.emptyList());
            queryResult.getRows(); returns(Arrays.asList(row, row, row, row));
            key.get(2); returns(JsonNodeFactory.instance.textNode("n"),
                    JsonNodeFactory.instance.textNode("s"),
                    JsonNodeFactory.instance.textNode("e"),
                    JsonNodeFactory.instance.textNode("w"));
        }};
        Coordinates c5 = docs.findUnusedCoordinate(c4);
        System.out.println(c4 + " --> " + c5);
        Assert.assertNotEquals("Coordinates should be different", c4, c5);
        Assert.assertNotEquals("North, South, East, West exits are present, move diagonally", c4.getY(), c5.getY());
        Assert.assertNotEquals("North, South, East, West exits are present, move diagonally", c4.getX(), c5.getX());
    }

}
