package org.gameontext.signed;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.gameontext.signed.SignedRequestMap.MLS_StringMap;
import org.gameontext.signed.SignedRequestMap.MVSO_StringMap;
import org.gameontext.signed.SignedRequestMap.MVSS_StringMap;
import org.gameontext.signed.SignedRequestMap.QueryParameterMap;
import org.junit.Assert;
import org.junit.Test;

public class SignedRequestMapTest {

    final String queryString = "owner=game-on.org&name=MugRoom&something=A+B+C&something=D/E/F&something=G%26H";
    final String startingMap = "{owner=[game-on.org], name=[MugRoom], something=[A+B+C, D/E/F, G%26H]}";
    final String smashedValue = "A+B+CD/E/FG%26H";

//    map_1 | QUERY: {owner=[game-on.org], name=[MugRoom], something=[A+B+C, D/E/F]}
//    map_1 | HEADERS: {Accept=[application/json], Cache-Control=[no-cache], connection=[keep-alive], content-type=[text/xml], gameon-date=[Wed, 25 May 2016 17:06:22 GMT], gameon-id=[game-on.org], gameon-signature=[+HFJ4H8ZK+VxmDLwOzJDV2MDrldFeLnmMrsEBI2p308=], Host=[map:9080], Pragma=[no-cache], User-Agent=[Apache CXF 3.0.3]}
//    map_1 | QUERY FETCH: A+B+CD/E/F

    public void assertMapGetBehavior(SignedRequestMap map) {
        Assert.assertEquals("Default value used for missing key", "missing", map.getAll("unknown", "missing"));
        Assert.assertEquals("Value should be concatenated in order", smashedValue, map.getAll("something", null));
    }

    public void assertMapAddBehavior(SignedRequestMap map) {
        String key = "newKey";
        map.putSingle(key, "firstValue");
        Assert.assertEquals("firstValue", map.getAll(key, "firstValue"));
        Assert.assertEquals(map.getFirst(key), map.getAll(key, "missing"));

        map.putSingle(key, "secondValue");
        Assert.assertEquals("secondValue should replace firstValue", "secondValue", map.getAll(key, "secondValue"));
        Assert.assertEquals(map.getFirst(key), map.getAll(key, "missing"));
    }

    @Test
    public void testMVSO_StringMap() {
        MultivaluedMap<String, Object> backing = new MultivaluedHashMap<>();
        backing.add("owner", "game-on.org");
        backing.add("name", "MugRoom");
        backing.add("something", "A+B+C");
        backing.add("something", "D/E/F");
        backing.add("something", "G%26H");
        Assert.assertEquals("Starting place should match", startingMap, backing.toString());

        MVSO_StringMap mvso = new MVSO_StringMap(backing);
        assertMapGetBehavior(mvso);
        assertMapAddBehavior(mvso);
    }

    @Test
    public void testMVSS_StringMap() {
        MultivaluedMap<String, String> backing = new MultivaluedHashMap<>();
        backing.add("owner", "game-on.org");
        backing.add("name", "MugRoom");
        backing.add("something", "A+B+C");
        backing.add("something", "D/E/F");
        backing.add("something", "G%26H");
        Assert.assertEquals("Starting place should match", startingMap, backing.toString());

        MVSS_StringMap mvss = new MVSS_StringMap(backing);
        assertMapGetBehavior(mvss);
        assertMapAddBehavior(mvss);
    }

    @Test
    public void testMLS_StringMap() {
        Map<String, List<String>> backing = new HashMap<>();
        backing.put("owner", Arrays.asList("game-on.org"));
        backing.put("name", Arrays.asList("MugRoom"));
        backing.put("something", Arrays.asList("A+B+C", "D/E/F", "G%26H"));
        Assert.assertEquals("Starting place should match", startingMap, backing.toString());

        MLS_StringMap mls = new MLS_StringMap(backing);
        assertMapGetBehavior(mls);
        assertMapAddBehavior(mls);
    }

    @Test(expected = IllegalStateException.class)
    public void testPutQueryParameterMap() {
        QueryParameterMap qp = new QueryParameterMap(queryString);
        assertMapAddBehavior(qp); // adding to read-only map will fail
    }


    @Test
    public void testQueryParameterMap() {
        QueryParameterMap qp = new QueryParameterMap(queryString);
        qp.getFirst("forceUnpack");
        Assert.assertEquals("Starting place should match", startingMap, qp.mvss.toString());
        assertMapGetBehavior(qp);

    }


}
