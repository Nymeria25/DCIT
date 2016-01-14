package distributed_system_rpc;

import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionUpdaterImpl implements ConnectionUpdaterService {

    public ConnectionUpdaterImpl(NodeIdentity nodeId) {
        network_ = new Network(nodeId);
        networkUpdateQueue_ = new ArrayDeque<>();
        sentenceUpdateQueue_ = new ArrayDeque<>();
        networkUpdate_ = false;
        algorithm_ = "";
        sentence_ = "";
    }
    
    
    // If the critical zone with the list of nodes is not accessed by other node
    // at the moment of call, returns true.
    // Otherwise, blocks the caller until the critical zone with the list of
    // nodes is free again and then returns true.
    @Override
    public boolean performNetworkUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        networkUpdateQueue_.add(nodeId);
        
        // Block the client while other node is performing updates.
        while(!(networkUpdate_ == false && networkUpdateQueue_.peek() == nodeId)) {}
        
        // The critical zone of connected nodes is free.
        // That is, nodeId is on top of the queue and no one is updating the
        // network at this time (networkUpdate_ = false).
        networkUpdate_ = true;        
        return true;
    }

    @Override
    public boolean doneNetworkUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        if (nodeId.equals(networkUpdateQueue_.peek())) {
            networkUpdate_ = false;
            networkUpdateQueue_.poll();
            return true;
        }
        return false;
    }
    
    @Override
    public boolean performSentenceUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        sentenceUpdateQueue_.add(nodeId);
        
        // Block the client while other node is performing updates.
        while(!(sentenceUpdate_ == false && sentenceUpdateQueue_.peek() == nodeId)) {}
        
        // The critical zone of connected nodes is free.
        // That is, nodeId is on top of the queue and no one is updating the
        // network at this time (sentenceUpdate_ = false).
        sentenceUpdate_ = true;        
        return true;
    }

    @Override
    public boolean doneSentenceUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        if (nodeId.equals(sentenceUpdateQueue_.peek())) {
            sentenceUpdate_ = false;
            sentenceUpdateQueue_.poll();
            return true;
        }
        return false;
    }
    
    public boolean ricartAgrawalaReq(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        network_.getGrantedAccess(nodeId);
        while (!network_.hasGrantedAccess()) {}
        
        return true;        
    }

    
    @Override
    public boolean readWriteReady(String algorithm) {
        try {
            algorithm_ = algorithm;
            network_.notifyReadWrite(algorithm_);
        } catch (MalformedURLException ex) {
            // do the do the konga
        }
        return true;
    }
       
    @Override
    public boolean startReadWrite(String algorithm) {
        algorithm_ = algorithm;
        return true;
    }
    
    @Override
    public String getReadWriteStatus() {
        return algorithm_;
    }
    
    @Override
    public String getSentence() {
        return sentence_;
    }
    
    @Override
    public String getSentenceFromMaster() {
        return network_.getSentenceFromMaster();
    }
    
    @Override
    public boolean writeSentence(String sentence) {
        sentence_ = sentence;
        return true;
    }
    
    @Override
    public boolean writeSentenceToMaster(String sentence) {
        network_.writeSentenceToMaster(sentence);
        return true;
    }
   
    @Override
    public boolean joinNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        try {
            network_.joinNode(nodeId);
        } catch (MalformedURLException ex) {
            // do the konga
        }
        return true;
    }
    
    public boolean addNodeToNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        try {
            network_.AddConnectionUpdater(nodeId);
        } catch (MalformedURLException ex) {
            // do the konga
        }
        return true;
    }
    
    public boolean signOff(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        network_.signOff(nodeId);
        return true;
    }
    @Override
    public boolean removeNodeFromNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        network_.removeNode(nodeId);
        return true;
    }
    
    @Override
    public Vector<String> getConnectedNodes() {
        Vector<String> connected = network_.getNodes();
        return connected;
    }
    
    @Override
    public boolean print() {
        Vector<String> connected = network_.getNodes();
        System.out.println(connected.size());
        for (String node : connected) {
            System.out.println(node);
        }
        return true;
    }

    // ------------------ Private  -------------------
    
    private String algorithm_;
    
    // Hashset of node identities of the current nodes in the network.
    private Network network_;
    
    // Used for centralised mutual exclusion.
    // Guards connectedNodes_.
    private boolean networkUpdate_, sentenceUpdate_;
    
    // Queue of the nodes who wait to perform an update to the network nodes.
    // Guards connectedNodes_.
    private Queue<NodeIdentity> networkUpdateQueue_, sentenceUpdateQueue_;
    
    private String sentence_;
 
    // Used for testing only.
    private volatile int index = 1;

}
