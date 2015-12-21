package distributed_system_rpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

public class RpcClient {

    RpcClient(NodeIdentity nodeServerNodeId, NodeIdentity primaryServerNodeId)
            throws MalformedURLException {
        connectionUpdaters_ = new ConcurrentHashMap<>();

        nodeServerNodeId_ = nodeServerNodeId;
        ClientFactory nodeServerFactory = CreateClientFactory(
                nodeServerNodeId_);
        nodeServer_ = (ConnectionUpdaterService) nodeServerFactory.
                newInstance(ConnectionUpdaterService.class);

        primaryServerNodeId_ = primaryServerNodeId;
        ClientFactory primaryServerFactory = CreateClientFactory(
                primaryServerNodeId_);
        primaryServer_ = (ConnectionUpdaterService) primaryServerFactory.
                newInstance(ConnectionUpdaterService.class);

        AddConnectionUpdater(nodeServerNodeId_, nodeServer_);
        primaryServer_.join(nodeServerNodeId_.toString());
        UpdateNodeList();
    }

    // Updates the hashmap of connected nodes.
    public final void UpdateNodeList() throws MalformedURLException {
        // Gets the list of connected nodes from the node with which we first
        // established the connection.

        try {
            Vector<String> nodes = primaryServer_.getConnectedNodes();
            HashSet<NodeIdentity> connectedNodesIds = new HashSet<>();
            for (String node : nodes) {
                NodeIdentity nodeId = new NodeIdentity(node);
                connectedNodesIds.add(nodeId);
            }

        // Removes the connection updater sevices from the hashmap that no
            // longer exist in the list of connected nodes (because nodes have
            // signed off).
            RemoveDisconnectedNodes(connectedNodesIds);

        // Creates new connection updater services for the new nodes and adds
            // them to the hashmap.
            AddNewlyConnectedNodes(connectedNodesIds);

        } catch (Exception e) {
            System.err.println("Primary server has disconnected. Selecting"
                    + " a new one.");

            connectionUpdaters_.remove(primaryServerNodeId_);

            if (connectionUpdaters_.size() > 0) {
                primaryServer_ = connectionUpdaters_.entrySet().iterator().next().
                        getValue();
                primaryServerNodeId_ = connectionUpdaters_.entrySet().iterator().
                        next().getKey();
            } else {
                System.err.println("No server available.");
            }
        }
    }

    // Runs the client console and reads/performs manual commands from keyboard.
    public void RunClientConsole() throws XmlRpcException,
            MalformedURLException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Command: ");
            String command = scanner.next();

            if ("$echo".equals(command)) {
                String message = scanner.next();
                this.ClientEcho(message);
            } else if ("$print".equals(command)) {
                nodeServer_.print();
            } else if ("$kill".equals(command)) {
                ClientSignOff();
                System.exit(0);
            }
        }
    }

    public void ClientSignOff() {
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            cuEntry.getValue().signOff(nodeServerNodeId_.toString());
        }
    }

    // Sends a message to all the nodes in the network. 
    public void ClientEcho(String message) throws XmlRpcException {
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            cuEntry.getValue().echo(cuEntry.getKey().toString(), message);
        }
    }

    // ------------------ Private  -------------------
    // Adds a new ConnectionUpdater object in the hashmap, for the specified
    // NodeIdentity.
    // If the node already exists in the hashmap of nodes, returns false and
    // leaves the hashmap untouched.
    private boolean AddConnectionUpdater(NodeIdentity nodeId) throws
            MalformedURLException {
        if (!connectionUpdaters_.containsKey(nodeId)) {
            ClientFactory factory = CreateClientFactory(nodeId);
            ConnectionUpdaterService cu = (ConnectionUpdaterService) factory.
                    newInstance(ConnectionUpdaterService.class);
            connectionUpdaters_.put(nodeId, cu);
            return true;
        }
        return false;
    }

    // Overloads the previous function. Here, takes nodeId and the corresponding
    // connection updater service for that node id.
    private boolean AddConnectionUpdater(NodeIdentity nodeId,
            ConnectionUpdaterService cu) throws
            MalformedURLException {
        if (!connectionUpdaters_.containsKey(nodeId)) {
            connectionUpdaters_.put(nodeId, cu);
            return true;
        }
        return false;
    }

    // For each newly connected node, creates a ConnectionUpdaterService and
    // adds it to the hashmap of connection updater services.
    private void AddNewlyConnectedNodes(HashSet<NodeIdentity> connectedNodesIds)
            throws MalformedURLException {
        boolean updated = false;
        for (NodeIdentity nodeId : connectedNodesIds) {
            if (AddConnectionUpdater(nodeId)) {
                updated = true;
            }
        }
        if (updated) {
            for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                    : connectionUpdaters_.entrySet()) {
                cuEntry.getValue().join(
                        nodeServerNodeId_.toString());
            }
        }
    }

    // Takes a hashset of connected nodes and checks whether the hashmap with 
    // connectionUpdaters_ contains any nodes which are not there. If that is
    // the case, it means that those nodes have disconnected in the meantime 
    // and we remove them.
    private void RemoveDisconnectedNodes(HashSet<NodeIdentity> connectedNodesIds) {
        HashSet<NodeIdentity> removableNodeIds = new HashSet<>();
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            NodeIdentity nodeId = cuEntry.getKey();
            if (!connectedNodesIds.contains(nodeId)) {
                removableNodeIds.add(nodeId);
            }
        }
        for (NodeIdentity nodeId : removableNodeIds) {
            connectionUpdaters_.remove(nodeId);
        }
    }

    private ClientFactory CreateClientFactory(NodeIdentity nodeId) throws
            MalformedURLException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setEnabledForExtensions(true);
        config.setServerURL(new URL("http", nodeId.getKey(), nodeId.getValue(),
                "xmlrpc"));

        // Creates an XmlRpcClient and instantiates a ClientFactory.
        XmlRpcClient rpcClient = new XmlRpcClient();
        rpcClient.setConfig(config);
        return new ClientFactory(rpcClient);
    }

    private final ConcurrentHashMap<NodeIdentity, ConnectionUpdaterService> connectionUpdaters_;
    // The connection updater service that works as the server function of this
    // node.
    ConnectionUpdaterService nodeServer_;
    // The first server of the network to which this node was connected (when
    // it was instantiated.
    ConnectionUpdaterService primaryServer_;
    private NodeIdentity nodeServerNodeId_, primaryServerNodeId_;
}
