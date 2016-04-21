package net.wasdev.gameon.map.couchdb.auth;

import java.util.Collections;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

import net.wasdev.gameon.map.models.ConnectionDetails;

@ApplicationScoped
public class ResourceAccessPolicyFactory {

	@Resource(lookup="systemId")
	private String systemId;
	@Resource(lookup="sweepId")
	private String sweepId;

	public ResourceAccessPolicyFactory() {}
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
    		return new AccessCertainResourcesPolicy(Collections.singleton(ConnectionDetails.class));
    	} else {
    		return new AccessOwnContentPolicy(user);
    	}
	}

}