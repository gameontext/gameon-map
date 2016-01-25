package org.gameon.map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This acts as an exception mapper: if/when this uncaught exception is thrown,
 * information will be packed as the response body to the user.
 */
@Provider
public class MapModificationException extends RuntimeException implements ExceptionMapper<MapModificationException> {

    private static final long serialVersionUID = 1L;
    final JsonNodeFactory factory = JsonNodeFactory.instance;

    final Response.Status status;
    final String message;
    final String moreInfo;

    public MapModificationException(String message) {
        this(message, null);
    }

    public MapModificationException(String message, String moreInfo) {
        this(Response.Status.INTERNAL_SERVER_ERROR, message, moreInfo);
    }

    public MapModificationException(Response.Status status, String message, String moreInfo) {
        this.message = message;
        this.status = status;
        this.moreInfo = moreInfo;
    }


    @Override
    public Response toResponse(MapModificationException exception) {
        ObjectNode objNode = factory.objectNode();
        objNode.put("status", status.getStatusCode());
        objNode.put("message", message);
        if ( moreInfo != null )
            objNode.put("more_info", moreInfo);

        return Response.status(status).entity(objNode).build();
    }

}
