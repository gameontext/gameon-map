package org.gameon.map;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.gameon.map.couchdb.MapRepository;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Provider
public class ErrorResponseMapper implements ExceptionMapper<Exception> {

    private static final long serialVersionUID = 1L;

    @Inject
    protected MapRepository mapRepository;

    @Override
    public Response toResponse(Exception exception) {
        ObjectNode objNode = mapRepository.mapper().createObjectNode();

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
            objNode.put("more_info", dne.getBody());

        } else if ( exception instanceof UpdateConflictException ) {
            status = Response.Status.CONFLICT;
        }

        objNode.put("status", status.getStatusCode());
        objNode.put("message", message);

        return Response.status(status).entity("YAY").build();
    }
}
