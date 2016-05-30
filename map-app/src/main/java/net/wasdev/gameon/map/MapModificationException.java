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

/**
 * This acts as an exception mapper: if/when this uncaught exception is thrown,
 * information will be packed as the response body to the user.
 */
public class MapModificationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Response.Status status;
    private final String moreInfo;

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
