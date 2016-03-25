/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
