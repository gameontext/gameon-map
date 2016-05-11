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
package net.wasdev.gameon.map.couchdb.auth;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

import net.wasdev.gameon.map.couchdb.SiteSwapper;
import net.wasdev.gameon.map.models.ConnectionDetails;

@ApplicationScoped
public class ResourceAccessPolicyFactory {

    @Resource(lookup = "systemId")
    private String systemId;
    @Resource(lookup = "sweepId")
    private String sweepId;

    public ResourceAccessPolicyFactory() {
    }

    ResourceAccessPolicyFactory(String systemId, String sweepId) {
        this.systemId = systemId;
        this.sweepId = sweepId;
    }

    public ResourceAccessPolicy createPolicyForUser(String user) {
        if (user == null) {
            return new NoAccessPolicy();
        } else if (systemId.equals(user)) {
            return new FullAccessPolicy();
        } else if (sweepId.equals(user)) {
     	    Collection<Class<?>> collection = new ArrayList<>();
    	    collection.add(ConnectionDetails.class);
    	    collection.add(SiteSwapper.class);
	    return new AccessCertainResourcesPolicy(collection);
        } else {
            return new AccessOwnContentPolicy(user);
        }
    }
}
