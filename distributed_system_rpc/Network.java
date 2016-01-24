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

    public int getSize() {
        return connectionUpdaters_.size();
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
            // TODO: maybe add back the try/catch?
                cuEntry.getValue().removeNodeFromNetwork(nodeId_.toString());
            
        }
       // RemoveFailedNodesFromNetwork();
    }

    public void removeNode(NodeIdentity nodeId) {
        connectionUpdaters_.remove(nodeId);
    }

    // Centralized Mutual Exclusion delegators
    public void performSentenceUpdate(NodeIdentity nodeId) {
        try {
            masterConnectionUpdater_.performSentenceUpdate(nodeId.toString());
        } catch (Exception e) {
            System.out.println("Master is no longer in the network.");
            ElectMasterNode();
        }
    }

    public void doneSentenceUpdate(NodeIdentity nodeId) {
        try {
            masterConnectionUpdater_.doneSentenceUpdate(nodeId.toString());
        } catch (Exception e) {
            System.out.println("Master is no longer in the network.");
            ElectMasterNode();
        }
    }

    // Ricart Agrawala delegators
    public void ricartAgrawalaReq(final int lamport, final String nodeIdp) {
        for (final Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            if (!cuEntry.getKey().toString().equals(nodeIdp)) {
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            System.out.println("Requesting access from " + 
                                    cuEntry.getKey().toString());
                            ConnectionUpdaterService cu = cuEntry.getValue();
                            cu.getAccess(lamport, nodeIdp);
                        } catch (Exception e) {
                            System.err.println("Ricart exception.");
                        }

                    }
                };
                thread.start();
            }
        }
    }

    public void sendOK(NodeIdentity nodeId) {
        System.out.println("NETWORK OK");
        ConnectionUpdaterService cu = connectionUpdaters_.get(nodeId);
        if (cu != null) {
            try {cu.OK(nodeId_.toString()); }
            catch (Exception e) {
                //
            }
        }
    }

    // -------
    public void notifyReadWrite(final String algorithm) {
        failedNodes_.clear();
        ElectMasterNode();

        for (final Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            // Tries to send a notification to this node. (The node might be
            // disconnected.)
            if (!cuEntry.getKey().equals(nodeId_)) {
                try {
                    System.out.println("Notify to start read/write for: "
                            + cuEntry.getKey().toString());

                    Thread thread = new Thread() {
                        public void run() {
                            cuEntry.getValue().readWrite(algorithm);
                        }
                    };
                    thread.start();

                } catch (Exception e) {
                    System.err.println("exception");
                    failedNodes_.add(cuEntry.getKey());
                }
            }
        }

        System.out.println("Notify myself! " + nodeId_.toString());
        connectionUpdaters_.get(nodeId_).readWrite(algorithm);

        RemoveFailedNodesFromNetwork();
    }

    // Ricart Agrawala
    public void getGrantedAccess(final int lamport, final NodeIdentity nodeId) {
        failedNodes_.clear();
        for (final Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            if (cuEntry.getKey().compareTo(nodeId_) != 0) {
                try {
                    Thread thread = new Thread() {
                        public void run() {
                            cuEntry.getValue().getAccess(lamport, nodeId.toString());
                        }
                    };
                    thread.start();
                } catch (Exception e) {
                    failedNodes_.add(cuEntry.getKey());
                }
            }
        }
        RemoveFailedNodesFromNetwork();
    }
    
    public Vector<String> getSentenceUpdateHistory(NodeIdentity nodeId) {
        Vector<String> v = new Vector<>();
        try {
            v = masterConnectionUpdater_.getSentenceUpdateHistory(nodeId.toString());
        } catch (Exception e) {
            System.out.println("Failed to get sentence update history.");
            // ElectMasterNode();
        }
        return v;
    }

    public String getSentenceFromMaster() {
        String sentence = "";
        try {
            sentence = masterConnectionUpdater_.getMasterSentence();
        } catch (Exception e) {
            System.out.println("Failed to get sentence.");
            // ElectMasterNode();
        }
        return sentence;
    }

    public void writeSentenceToMaster(String sentence) {
        try {
            System.out.println("Network writes sentence to master: " + masterNodeId_.toString());
            masterConnectionUpdater_.writeMasterSentence(nodeId_.toString(), sentence);
        } catch (Exception e) {
            masterConnectionUpdater_.writeMasterSentence(nodeId_.toString(), sentence);
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
                "mesh.rem"));

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

    public String getMasterId() {
        return masterNodeId_.toString();
    }
    public void setMaster(NodeIdentity nodeId) {
        masterNodeId_ = nodeId;
        masterConnectionUpdater_ = connectionUpdaters_.get(masterNodeId_);
    }

    public void ElectMasterNode() {
        System.out.println("Electing master node!");
        failedNodes_.clear();

        boolean foundPotentialMaster = false;

        for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                : connectionUpdaters_.entrySet()) {
            try {
                final ConnectionUpdaterService cu = cuEntry.getValue();
                if (cuEntry.getKey().compareTo(nodeId_) > 0 && cu.isAlive()) {
                    foundPotentialMaster = true;
                    Thread thread = new Thread() {
                        public void run() {
                            cu.electMaster();
                        }
                    };
                    thread.start();
                }
            } catch (Exception e) {
                failedNodes_.add(cuEntry.getKey());
            }
        }
        RemoveFailedNodesFromNetwork();
        if (foundPotentialMaster) {
            System.out.println("Found potential master.");
            masterNodeId_ = null;
            while (masterNodeId_ == null) {
                try {
                    System.out.println("Waiting for a response from master.");
                    Thread.sleep(generateRandomWaitingTime(100, 200));
                } catch (InterruptedException ex) {
                    // nothing
                }
            }
        } else {
            for (Map.Entry<NodeIdentity, ConnectionUpdaterService> cuEntry
                    : connectionUpdaters_.entrySet()) {
                try {
                    cuEntry.getValue().setMaster(nodeId_.toString());
                } catch (Exception e) {
                    // nothing
                }
            }
        }
    }

    private int generateRandomWaitingTime(int Min, int Max) {
        return Min + (int) (Math.random() * ((Max - Min) + 1));
    }

    private final ConcurrentSkipListMap<NodeIdentity, ConnectionUpdaterService> connectionUpdaters_;
    ConnectionUpdaterService masterConnectionUpdater_;
    private HashSet<NodeIdentity> failedNodes_;
    NodeIdentity nodeId_, masterNodeId_;
}
