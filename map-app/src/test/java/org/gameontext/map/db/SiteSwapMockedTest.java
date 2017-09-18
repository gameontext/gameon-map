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
package org.gameontext.map.db;

import java.util.Collections;
import java.util.List;

import org.ektorp.CouchDbConnector;
import org.gameontext.map.MapModificationException;
import org.gameontext.map.auth.AccessCertainResourcesPolicy;
import org.gameontext.map.auth.SiteSwapPermission;
import org.gameontext.map.kafka.Kafka;
import org.gameontext.map.model.Coordinates;
import org.gameontext.map.model.Site;
import org.gameontext.map.model.SiteCoordinates;
import org.gameontext.map.model.SiteSwap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

public class SiteSwapMockedTest {

    private static final AccessCertainResourcesPolicy swapRoomsAccessPolicy = new AccessCertainResourcesPolicy(Collections.singleton(SiteSwapPermission.class));
    private static final String owner = "testOwner";

    @Mocked
    CouchDbConnector dbc;

    @Mocked
    Kafka kafka;

    MapRepository repo;
    ObjectWriter debugWriter;

    Site site1;
    Site site2;

    @Rule
    public TestName test = new TestName();

    @Before
    public void before() {
        System.out.println("\n====== " + test.getMethodName());

        // instance only for mocking.
        ObjectMapper mapper = new ObjectMapper();
        final SiteDocuments anyInstance = new SiteDocuments(null, mapper);

        new Expectations(SiteDocuments.class) {{
            anyInstance.getByCoordinate(0, 1); result = new Site(0, 1);
            anyInstance.getByCoordinate(0, -1); result = new Site(0, -1);
            anyInstance.getByCoordinate(1, 0); result = new Site(1, 0);
            anyInstance.getByCoordinate(-1, 0); result = new Site(-1, 0);
        }};

        repo = new MapRepository();
        repo.db = dbc;
        repo.kafka = kafka;
        repo.postConstruct();

        debugWriter = repo.mapper.writerWithDefaultPrettyPrinter();

        site1 = new Site(1, 1);
        site1.setId("A");

        site2 = new Site(-1, -1);
        site2.setId("B");
    }

    @Test
    public void testSwapRooms() {
        Coordinates coord1 = new Coordinates(1, 1);
        Coordinates coord2 = new Coordinates(-1, -1);

        SiteCoordinates sc1 = new SiteCoordinates("A", coord1);
        SiteCoordinates sc2 = new SiteCoordinates("B", coord2);
        SiteSwap swap = new SiteSwap(sc1, sc2);

        new Expectations() {{
            dbc.get(Site.class, "A"); returns(site1);
            dbc.get(Site.class, "B"); returns(site2);
        }};

        List<Site> result = repo.swapSites(swapRoomsAccessPolicy, owner, swap);
        Assert.assertEquals("Returned list should have two elements", 2, result.size());

        Site result1 = result.get(0);
        Assert.assertEquals("A", result1.getId());
        Assert.assertEquals(coord2, result1.getCoord());

        Site result2 = result.get(1);
        Assert.assertEquals("B", result2.getId());
        Assert.assertEquals(coord1, result2.getCoord());

        new Verifications() {{
            dbc.get(Site.class, "A"); times = 1;
            dbc.get(Site.class, "B"); times = 1;

            List<Site> bulk;
            dbc.executeBulk(bulk = withCapture()); times = 1;
            Assert.assertEquals("List sent to DB is same as list returned", result, bulk);
        }};
    }

    @Test
    public void testSwapSameRoom() {
        Coordinates coord1 = new Coordinates(1, 1);

        SiteCoordinates sc1 = new SiteCoordinates("A", coord1);
        SiteSwap swap = new SiteSwap(sc1, sc1);

        MapModificationException e = null;
        try {
            repo.swapSites(swapRoomsAccessPolicy, owner, swap);
        } catch (MapModificationException ex){
            e = ex;
        }

        Assert.assertNotNull(e);
        String expectedMessage = "Unable to swap sites";
        String actualMessage = e.getMessage();
        Assert.assertEquals(expectedMessage, actualMessage);
        Assert.assertTrue(e.getMoreInfo().contains("itself"));

    }

    @Test
    public void testSwapAlreadySwappedRooms() {
        Coordinates coord1 = new Coordinates(1, 0);
        Coordinates coord2 = new Coordinates(-1, -1);

        SiteCoordinates sc1 = new SiteCoordinates("A", coord1);
        SiteCoordinates sc2 = new SiteCoordinates("B", coord2);
        SiteSwap swap = new SiteSwap(sc1, sc2);

        new Expectations() {{
            dbc.get(Site.class, "A"); returns(site1);
            dbc.get(Site.class, "B"); returns(site2);
        }};

        MapModificationException e = null;
        try {
            repo.swapSites(swapRoomsAccessPolicy, owner, swap);
        } catch (MapModificationException ex){
            e = ex;
        }
        Assert.assertNotNull(e);
        String expectedMessage = "Unable to swap sites";
        String actualMessage = e.getMessage();
        Assert.assertEquals(expectedMessage, actualMessage);
        Assert.assertTrue(e.getMoreInfo().contains("moved"));
    }

}
