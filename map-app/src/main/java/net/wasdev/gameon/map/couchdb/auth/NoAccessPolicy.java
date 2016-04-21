package net.wasdev.gameon.map.couchdb.auth;

public class NoAccessPolicy implements ResourceAccessPolicy {

	@Override
	public boolean isAuthorisedToView(String resourceOwnedBy, Class<?> resourceType) {
		return false;
	}

}
