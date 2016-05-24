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
package net.wasdev.gameon.map.auth;

import java.io.IOException;
import java.util.logging.Level;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import net.wasdev.gameon.map.Log;

public class SignedRequestFilter implements ContainerRequestFilter {

    private final PlayerClient playerClient;
    private final SignedRequestTimedCache timedCache;

    public SignedRequestFilter(PlayerClient playerClient, SignedRequestTimedCache timedCache) {
        this.playerClient = playerClient;
        this.timedCache = timedCache;

        if ( playerClient == null || timedCache == null ) {
            Log.log(Level.SEVERE, this, "Required resources are not available: playerClient={0}, timedCache={1}", playerClient, timedCache);
            throw new IllegalStateException("Required resources are not available");
        }
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.container.ContainerRequestFilter#filter(javax.ws.rs.container.ContainerRequestContext)
     * @see SignedRequestInterceptor
     * @see SignedRequestFeature
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        WebApplicationException invalidHmacEx = null;

        SignedRequestHmac hmac = new SignedRequestHmac(requestContext);

        String method = requestContext.getMethod();

        if ( hmac.getUserId() == null && "GET".equals(method) ) {
            // no validation required for GET requests. If an ID isn't provided,
            // then we won't do validation and will just return.
            Log.log(Level.FINEST, this, "FILTER: NO ID-- NO VERIFICATION, {0}", hmac);
            return;
        }

        requestContext.setProperty("player.id", hmac.getUserId());

        try {
            hmac.checkDuplicate(method, timedCache);
            hmac.checkExpiry();
            hmac.precheck(playerClient);

            if ( hmac.requestBodyRequired() ) {
                // set this as a property on the request context, and wait for the
                // signed request interceptor to catch the request
                // @see SignedRequestInterceptor as assigned by SignedRequestFeature
                requestContext.setProperty("SignedRequestHmac", hmac);
            } else {
                hmac.validate();
            }
        } catch(WebApplicationException ex) {
            invalidHmacEx = ex;
        } catch(Exception e) {
            invalidHmacEx = new WebApplicationException("Unexpected exception validating signature",
                    e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        Log.log(Level.FINEST, this, "FILTER: {0} {1}", invalidHmacEx, hmac);

        if ( invalidHmacEx != null ) {
            // STOP!! turn this right around with the bad response
            requestContext.abortWith(invalidHmacEx.getResponse());
        }
    }
}
