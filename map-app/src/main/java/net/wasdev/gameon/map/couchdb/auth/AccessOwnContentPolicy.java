package net.wasdev.gameon.map.couchdb.auth;

public class AccessOwnContentPolicy implements ResourceAccessPolicy {

    private String user;

    public AccessOwnContentPolicy(String user) {
        this.user = user;
    }

    @Override
    public boolean isAuthorisedToView(String resourceOwnedBy, Class<?> resourceType) {
        return user.equals(resourceOwnedBy);
    }

}
