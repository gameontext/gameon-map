package org.gameon.map;

import javax.ws.rs.core.Response;

/**
 * This acts as an exception mapper: if/when this uncaught exception is thrown,
 * information will be packed as the response body to the user.
 */
public class MapModificationException extends RuntimeException {

    final Response.Status status;
    final String moreInfo;

    public MapModificationException(String message) {
        this(message, null);
    }

    public MapModificationException(String message, String moreInfo) {
        this(Response.Status.INTERNAL_SERVER_ERROR, message, moreInfo);
    }

    public MapModificationException(Response.Status status, String message, String moreInfo) {
        super(message);
        this.status = status;
        this.moreInfo = moreInfo;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getMoreInfo() {
        return moreInfo;
    }
}
