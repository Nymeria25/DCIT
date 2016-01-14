/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributed_system_rpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

public class Network {

    public Network(NodeIdentity nodeId) {
        NodeIdentityComparator comparator = new NodeIdentityComparator();
        connectionUpdaters_ = new ConcurrentSkipListMap<>(comparator);
        failedNodes_ = new HashSet<>();
        nodeId_ = nodeId;
        try {
            AddConnectionUpdater(nodeId);
        } catch (MalformedURLException ex) {
            // trolo
        }
    }

    public void joinNode(NodeIdentity nodeId) throws MalformedURLException {
        failedNodes_.clear();

        ClientFactory factory = CreateClientFactory(nodeId);
        ConnectionUpdaterService cu = (ConnectionUpdaterService) factory.
                newInstance(ConnectionUpdaterService.class);
        Vector<String> nodes = new Vector<>();
        nodes = cu.getConnectedNodes();

        for (String node : nodes) {
            NodeIdentity nodeIdp = new NodeIdentity(node);
            AddConnectionUpdater(nodeIdp);
        }

        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            try {
                cuEntry.getValue().addNodeToNetwork(nodeId_.toString());
            } catch (Exception e) {
                failedNodes_.add(cuEntry.getKey());
            }
        }
        RemoveFailedNodesFromNetwork();
    }

    public Vector<String> getNodes() {
        Vector<String> nodes = new Vector<>();
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            nodes.add(cuEntry.getKey().toString());
        }
        return nodes;
    }

    public void signOff(NodeIdentity nodeId) {
        failedNodes_.clear();

        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            try {
                cuEntry.getValue().removeNodeFromNetwork(nodeId_.toString());
            } catch (Exception e) {
                failedNodes_.add(cuEntry.getKey());
            }
        }
        RemoveFailedNodesFromNetwork();
    }

    public void removeNode(NodeIdentity nodeId) {
        connectionUpdaters_.remove(nodeId);
    }

    public void notifyReadWrite(String algorithm) throws MalformedURLException {
        failedNodes_.clear();
        ElectMasterNode();

        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            // Tries to send a notification to this node. (The node might be
            // disconnected.)
            if (!cuEntry.getKey().equals(nodeId_) && !cuEntry.getKey().equals(masterNodeId_)) {
                try {
                    cuEntry.getValue().startReadWrite(algorithm);
                } catch (Exception e) {
                    failedNodes_.add(cuEntry.getKey());
                }
            }
        }

        connectionUpdaters_.get(nodeId_).startReadWrite(algorithm);

        RemoveFailedNodesFromNetwork();
    }

    
    // Ricart Agrawala
    public void getGrantedAccess(NodeIdentity nodeId) {
        // TODO: IMPLEMENTATION
    }
    
    public boolean hasGrantedAccess() {
        // TODO: IMPLEMENTATION
        return true;
    }
    
    public String getSentenceFromMaster() {
        String sentence = "";
        try {
            sentence = masterConnectionUpdater_.getSentence();
        } catch (Exception e) {
            ElectMasterNode();
        }
        return sentence;
    }

    public void writeSentenceToMaster(String sentence) {
        try {
            masterConnectionUpdater_.writeSentence(sentence);
        } catch (Exception e) {
            ElectMasterNode();
        }
    }

    // Adds a new ConnectionUpdater object in the hashmap, for the specified
    // NodeIdentity.
    // If the node already exists in the hashmap of nodes, returns false and
    // leaves the hashmap untouched.
    public final boolean AddConnectionUpdater(NodeIdentity nodeId) throws
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

    private void RemoveFailedNodesFromNetwork() {
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            try {
                for (NodeIdentity node : failedNodes_) {
                    cuEntry.getValue().removeNodeFromNetwork(node.toString());
                }
            } catch (Exception e) {
                // DO NOTHING HERE.
            }
        }
        failedNodes_.clear();
    }

    private void ElectMasterNode() {
        if (connectionUpdaters_.size() > 0) {
            Map.Entry<NodeIdentity, ConnectionUpdaterService> entry = 
                    connectionUpdaters_.entrySet().iterator().next();
            masterNodeId_ = entry.getKey();
            masterConnectionUpdater_ = entry.getValue();
        }
    }

    private final ConcurrentSkipListMap<NodeIdentity, ConnectionUpdaterService> connectionUpdaters_;
    ConnectionUpdaterService masterConnectionUpdater_;
    private HashSet<NodeIdentity> failedNodes_;
    NodeIdentity nodeId_, masterNodeId_;
}
