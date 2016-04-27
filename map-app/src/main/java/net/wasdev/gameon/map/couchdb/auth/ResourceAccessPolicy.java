package net.wasdev.gameon.map.couchdb.auth;

public interface ResourceAccessPolicy {

    public boolean isAuthorisedToView(String resourceOwnedBy, Class<?> resourceType);

}
