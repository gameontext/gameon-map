package org.gameon.map.couchdb;

import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.gameon.map.Log;
import org.gameon.map.models.Node;
import org.gameon.map.models.RoomInfo;

@ApplicationScoped
public class MapRepository {
    @Resource(name = "couchdb/connector")
    protected CouchDbInstance db;

    protected NodeDocuments nodes;

    @PostConstruct
    private void postConstruct() {
        String dbname = "map_repository";

        try {
            // Connect to the database with the specified
            CouchDbConnector dbc = db.createConnector(dbname, false);
            Log.log(Level.INFO, this, "Connected to {0}", dbname);

            // Ensure required views exist
            nodes = new NodeDocuments(Node.class, dbc);
        } catch (Exception e) {
            // Log the warning, and then re-throw to prevent this class from going into service,
            // which will prevent injection to the Health check, which will make the app stay down.
            Log.log(Level.WARNING, this, "Unable to connect to database", e);
            throw e;
        }
    }

    /**
     * Connect/Add/Place a new room into the Map
     *
     * @param newRoom Room or Suite to add
     * @return Wired node containing the room or Suite
     */
    public Node connectRoom(RoomInfo newRoom) {
        System.out.println("ADD NEW ROOM: " + newRoom);

        Node emptyNode = nodes.getEmptyNode();
        Log.log(Level.INFO, this, "Empty node: {0}", emptyNode);

        emptyNode.setInfo(newRoom);

        // TODO Auto-generated method stub
        return new Node();
    }



}
