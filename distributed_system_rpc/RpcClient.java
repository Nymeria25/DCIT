package distributed_system_rpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

public class RpcClient {

    RpcClient(NodeIdentity nodeServerNodeId, NodeIdentity primaryServerNodeId)
            throws MalformedURLException {
        NodeIdentityComparator comparator = new NodeIdentityComparator();
        connectionUpdaters_ = new ConcurrentSkipListMap<>(comparator);

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
        JoinNetworkImpl(primaryServer_, nodeServerNodeId_);
        // primaryServer_.joinNetwork(nodeServerNodeId_.toString());
        UpdateNetwork();
    }

    // Updates the hashmap of connected nodes and elects the master node.
    public final void UpdateNetwork() throws MalformedURLException {
        // Gets the list of connected nodes from the node with which we first
        // established the connection.

        try {
            Vector<String> nodes = primaryServer_.getConnectedNodes();
            HashSet<NodeIdentity> connectedNodesIds = new HashSet<>();
            for (String node : nodes) {
                NodeIdentity nodeId = new NodeIdentity(node);
                connectedNodesIds.add(nodeId);
            }

            // Removes the connection updater services from the hashmap that no
            // longer exist in the list of connected nodes (because nodes have
            // signed off).
            RemoveInexistentNodes(connectedNodesIds);

            // Creates new connection updater services for the new nodes and adds
            // them to the hashmap.
            AddNewlyConnectedNodes(connectedNodesIds);
            
            // Elects the master node of the network.
            UpdateMasterNode();

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
            } else if ("$print_master".equals(command)) {
                System.out.println("MASTER: " + masterServerNodeId_.toString());
            } else if ("$kill".equals(command)) {
                ClientSignOff();
                System.exit(0);
            }
        }
    }

    public void ClientSignOff() throws MalformedURLException {
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            try {
                RemoveFromNetworkImpl(cuEntry.getValue(), nodeServerNodeId_);
               // cuEntry.getValue().removeFromNetwork(nodeServerNodeId_.toString());
            } catch (Exception e) {
                ReportFailureToNetworkServers(cuEntry.getKey());
            }
        }
    }

    // Sends a message to all the nodes in the network.
    public void ClientEcho(String message) throws XmlRpcException, MalformedURLException {
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            // Tries to send a request to this node. (The node might be
            // disconnected.)
            try {
                cuEntry.getValue().echo(cuEntry.getKey().toString(), message);
            } catch(Exception e) {
                System.err.println(cuEntry.getKey().toString() +
                        " has disconnected. Echo failed.");
                
                // Reports failure to all the non-crashed nodes.
                System.out.println("Reporting " + cuEntry.getKey().toString());
                ReportFailureToNetworkServers(cuEntry.getKey());
            } 
        }
    }

    // ------------------ Private  -------------------
    
    private void JoinNetworkImpl(ConnectionUpdaterService cu, NodeIdentity ni) {
        cu.performNetworkUpdate(nodeServerNodeId_.toString());
        cu.joinNetwork(ni.toString());
        cu.doneNetworkUpdate(nodeServerNodeId_.toString());
    }
    
    private void RemoveFromNetworkImpl(ConnectionUpdaterService cu, NodeIdentity ni) {
        cu.performNetworkUpdate(nodeServerNodeId_.toString());
        cu.removeFromNetwork(ni.toString());
        cu.doneNetworkUpdate(nodeServerNodeId_.toString());
    }
    
    
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
                JoinNetworkImpl(cuEntry.getValue(), nodeServerNodeId_);
             //   cuEntry.getValue().joinNetwork(
              //          nodeServerNodeId_.toString());
            }
        }
    }

    // Takes a hashset of connected nodes and checks whether the hashmap with 
    // connectionUpdaters_ contains any nodes which are not there. If that is
    // the case, it means that those nodes have disconnected in the meantime 
    // and we remove them.
    private void RemoveInexistentNodes(HashSet<NodeIdentity> connectedNodesIds) {
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
    
    private void ReportFailureToNetworkServers(NodeIdentity nodeId) throws 
            MalformedURLException {
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            // Tries to send a request to remove nodeId from the network. (The 
            // server to which we send the request might be disconnected.)
            try {
                RemoveFromNetworkImpl(cuEntry.getValue(), nodeId);
              //  cuEntry.getValue().removeFromNetwork(nodeId.toString());
            } catch(Exception e) {
                System.err.println(cuEntry.getKey().toString() +
                        " has disconnected or failed.");
            } 
        }
        UpdateNetwork();
    }
    
    private void UpdateMasterNode() {
        if (connectionUpdaters_.size() > 0) {
                masterServer_ = connectionUpdaters_.entrySet().iterator().next().
                        getValue();
                masterServerNodeId_ = connectionUpdaters_.entrySet().iterator().
                        next().getKey();
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

    private final ConcurrentSkipListMap<NodeIdentity, ConnectionUpdaterService> 
            connectionUpdaters_;
    // The connection updater service that works as the server function of this
    // node.
    ConnectionUpdaterService nodeServer_;
    // The first server of the network to which this node was connected (when
    // it was instantiated.
    ConnectionUpdaterService primaryServer_, masterServer_;
    private NodeIdentity nodeServerNodeId_, primaryServerNodeId_, 
            masterServerNodeId_;
}
