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
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import net.wasdev.gameon.map.Log;

public class SignedRequestInterceptor implements ReaderInterceptor {

    public SignedRequestInterceptor() {
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.ext.ReaderInterceptor#aroundReadFrom(javax.ws.rs.ext.ReaderInterceptorContext)
     */
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {

        SignedRequestHmac hmac = (SignedRequestHmac) context.getProperty("SignedRequestHmac");

        if ( hmac.requestBodyRequired()) {
            // Fully read request body
            hmac.readRequestBody(context.getInputStream());

            // Validate HMAC signature (including body hash)
            WebApplicationException invalidHmacEx = null;

            try {
                hmac.validate();
            } catch(WebApplicationException ex) {
                invalidHmacEx = ex;
            }

            Log.log(Level.FINEST, this, "INTERCEPTOR: id={0}"
                    + ", date={1}"
                    + ", hash={2}"
                    + ", bodyHash={3}"
                    + ", exception={4}",
                    hmac.getUserId(),
                    hmac.dateString,
                    hmac.hmacHeader,
                    hmac.bodyHashHeader,
                    invalidHmacEx);

            if ( invalidHmacEx != null ) {
                throw invalidHmacEx;
            }

            // we've read the body in, set the stream for the context to read from
            // what we read...
            context.setInputStream(hmac.getBodyInputStream());
        }
        return context.proceed();
    }
}
