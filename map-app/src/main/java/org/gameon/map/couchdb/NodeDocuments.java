package org.gameon.map.couchdb;

import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;
import org.gameon.map.models.Node;

/**
 * Repository tracking and working with Nodes in the map
 */
@View( name = "all", file = "allNodes.js")
public class NodeDocuments extends CouchDbRepositorySupport<Node> {

    final ViewQuery emptyNodes;
    final ViewQuery emptyNode;

    protected NodeDocuments(Class<Node> type, CouchDbConnector db) {
        super(type, db);

        emptyNodes = new ViewQuery().designDocId("_design/Node").viewName("empty_nodes");
        emptyNode = new ViewQuery().designDocId("_design/Node").viewName("empty_nodes").limit(1);
    }

    public void createEmptyNodes(Node connectedNode) {

    }

    @View( name = "empty_nodes", file = "emptyNodes.js")
    public List<Node> getEmptyNodes() {
        return db.queryView(emptyNodes, Node.class);
    }

    public Node getEmptyNode() {
        List<Node> nodes = db.queryView(emptyNode, Node.class);
        if ( nodes.isEmpty() )
            return null;
        else
            return nodes.get(0);
    }
}
