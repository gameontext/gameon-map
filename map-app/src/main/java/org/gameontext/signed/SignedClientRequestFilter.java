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
package org.gameontext.signed;

import java.io.IOException;
import java.util.logging.Level;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;

public class SignedClientRequestFilter implements ClientRequestFilter {

    final String userId;
    final String secret;

    public SignedClientRequestFilter(String userId, String secret) {
        if (secret == null)
            throw new NullPointerException("NULL secret");
        this.userId = userId;
        this.secret = secret;
    }


    /* (non-Javadoc)
     * @see javax.ws.rs.container.ClientRequestFilter#filter(javax.ws.rs.container.ClientRequestContext)
     * @see SignedReaderInterceptor
     * @see SignedRequestFeature
     */
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        WebApplicationException invalidHmacEx = null;

        SignedRequestHmac hmac = new SignedRequestHmac(userId, secret, requestContext);

        String method = requestContext.getMethod();

        if ( userId == null && "GET".equals(method) ) {
            // no validation required for GET requests. If an ID isn't provided,
            // then we won't do validation and will just return.
            SignedRequestFeature.writeLog(Level.FINEST, this, "FILTER: NO ID-- NO VERIFICATION, {0}", hmac);
            return;
        }

        try {
            hmac.prepareForSigning(secret, requestContext.getHeaders());

            if ( hmac.requestBodyRequired() ) {
                // set this as a property on the request context, and wait for the
                // signed request interceptor to catch the request
                // @see SignedReaderInterceptor as assigned by SignedRequestFeature
                requestContext.setProperty("SignedRequestHmac", hmac);
            } else {
                hmac.signRequest(requestContext.getHeaders());
            }
        } catch(WebApplicationException ex) {
            invalidHmacEx = ex;
        } catch(Exception e) {
            invalidHmacEx = new WebApplicationException("Unexpected exception validating signature",
                    e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        SignedRequestFeature.writeLog(Level.FINEST, this, "FILTER: {0} {1}", invalidHmacEx, hmac);

        if ( invalidHmacEx != null ) {
            // STOP!! turn this right around with the bad response
            requestContext.abortWith(invalidHmacEx.getResponse());
        }
    }
}
