package net.wasdev.gameon.map.couchdb.auth;

public class FullAccessPolicy implements ResourceAccessPolicy {

	@Override
	public boolean isAuthorisedToView(String resourceOwnedBy, Class<?> resourceType) {
		return true;
	}

}
