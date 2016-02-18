package net.wasdev.gameon.map;

import javax.ws.rs.core.Response;

import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.wasdev.gameon.map.ErrorResponseMapper;
import net.wasdev.gameon.map.MapModificationException;

@Ignore
public class TestErrorResponseMapper {

    ObjectMapper om;
    ErrorResponseMapper exMapper;

    @Before
    public void before() throws Exception {
        om = new ObjectMapper();
        exMapper = new ErrorResponseMapper();
        exMapper.mapper = om;
    }

    ObjectNode getResponse(Exception e, int statusCode) {
        Response resp = exMapper.toResponse(e);

        Assert.assertEquals("Generic exception should result in " + statusCode + ": " + resp,
                statusCode, resp.getStatus());

        Object body = resp.getEntity();
        Assert.assertNotNull("Response should have a body", body);

        return null;
    }

    @Test
    public void testException() {
        ObjectNode node = getResponse(new Exception("TestException"), 500);

    }

    @Test
    public void testMapModificationException() {
        ObjectNode node = getResponse(new MapModificationException("TestException"), 500);
    }


    @Test
    public void testDocumentNotFoundException() {
        ObjectNode node = getResponse(new DocumentNotFoundException("TestException"), 400);
    }

    @Test
    public void testUpdateConflictException() {
        ObjectNode node = getResponse(new UpdateConflictException("TestDocument", "Revision"), 204);
    }


    @Test
    public void testJsonParseException() {
        ObjectNode node = getResponse(new JsonParseException("TestException", JsonLocation.NA), 402);
    }


    @Test
    public void testJsonMappingException() {
        ObjectNode node = getResponse(new JsonMappingException("TestException"), 402);
    }
}
