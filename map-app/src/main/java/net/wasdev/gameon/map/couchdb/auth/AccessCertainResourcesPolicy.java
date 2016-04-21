package net.wasdev.gameon.map.couchdb.auth;

import java.util.Collection;
import java.util.HashSet;

public class AccessCertainResourcesPolicy implements ResourceAccessPolicy {

	private final Collection<Class<?>> authorisedToView;
	public AccessCertainResourcesPolicy(Collection<Class<?>> authorisedToView) {
		this.authorisedToView = new HashSet<Class<?>>(authorisedToView);
	}

	@Override
	public boolean isAuthorisedToView(String resourceOwnedBy, Class<?> resourceType) {
		return authorisedToView.contains(resourceType);
	}
	
}
