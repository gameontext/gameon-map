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

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class SignedRequestFeature implements DynamicFeature {

    /** CDI injection of client for Player CRUD operations */
    @Inject
    PlayerClient playerClient;

    /** CDI injection of client for Player CRUD operations */
    @Inject
    SignedRequestTimedCache timedCache;

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        SignedRequest sr = resourceInfo.getResourceMethod().getAnnotation(SignedRequest.class);
        if ( sr == null ) {
            sr = resourceInfo.getResourceClass().getAnnotation(SignedRequest.class);
        }
        if ( sr == null )
            return;

        context.register(new SignedRequestFilter(playerClient, timedCache));

        GET get = resourceInfo.getResourceMethod().getAnnotation(GET.class);
        DELETE delete = resourceInfo.getResourceMethod().getAnnotation(DELETE.class);

        if ( get == null && delete == null ) {
            // Signed requests only for messages with bodies!
            context.register(new SignedRequestInterceptor());
        }
    }
}
