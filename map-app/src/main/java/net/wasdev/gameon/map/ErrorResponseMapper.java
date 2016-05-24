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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.wasdev.gameon.map.db.MapRepository;

@Provider
@ApplicationScoped
public class ErrorResponseMapper implements ExceptionMapper<Exception> {

    @Inject
    protected MapRepository mapRepository;

    protected ObjectMapper mapper;

    @PostConstruct
    protected void postConstruct() {
        mapper = mapRepository.mapper();
        if ( mapper == null )
            mapper = new ObjectMapper();
    }

    @Override
    public Response toResponse(Exception exception) {

        ObjectNode objNode = mapper.createObjectNode();

        Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
        String message = exception.getMessage();

        if ( exception instanceof MapModificationException ) {
            MapModificationException mme = (MapModificationException) exception;
            status = mme.getStatus();
            message = mme.getMessage();
            String moreInfo = mme.getMoreInfo();
            if ( moreInfo != null )
                objNode.put("more_info", moreInfo);

        } else if ( exception instanceof DocumentNotFoundException ) {
            DocumentNotFoundException dne = (DocumentNotFoundException) exception;
            status = Response.Status.NOT_FOUND;
            message = dne.getPath();
            objNode.set("more_info", dne.getBody());

        } else if ( exception instanceof UpdateConflictException ) {
            status = Response.Status.CONFLICT;

        } else if ( exception instanceof JsonParseException ) {
            status = Response.Status.BAD_REQUEST;

        } else if ( exception instanceof JsonMappingException ) {
            status = Response.Status.BAD_REQUEST;
        }

        objNode.put("status", status.getStatusCode());
        objNode.put("message", message);

        return Response.status(status).entity(objNode.toString()).build();
    }
}
