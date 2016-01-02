package distributed_system_rpc;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Vector;

public class ConnectionUpdaterImpl implements ConnectionUpdaterService {

    public ConnectionUpdaterImpl() {
        connectedNodes_ = new HashSet<>();
        networkUpdateQueue_ = new ArrayDeque<>();
        networkUpdate_ = false;
    }
    
    
    // If the critical zone with the list of nodes is not accessed by other node
    // at the moment of call, returns true.
    // Otherwise, blocks the caller until the critical zone with the list of
    // nodes is free again and then returns true.
    @Override
    public boolean performNetworkUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        networkUpdateQueue_.add(nodeId);
        
       /* System.out.println("Trying to update");
        System.out.println("networkUpdate_ = " + networkUpdate_);
        System.out.println("Top of queue = " + networkUpdateQueue_.peek().toString());
               */
        
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
    public boolean joinNetwork(String nodeIdp) {
        addConnectedNode(nodeIdp);
        return true;
    }
    
    @Override
    public boolean removeFromNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        connectedNodes_.remove(nodeId);
        return true;
    }
    
    @Override
    public Vector<String> getConnectedNodes() {
        Vector<String> connected = new Vector<>();
        for (NodeIdentity nodeId : connectedNodes_) {
            connected.add(nodeId.toString());
        }
        return connected;
    }
    
    @Override
    public boolean print() {
        for (NodeIdentity nodeId : connectedNodes_) {
            System.out.println(nodeId.toString());
        }
        return true;
    }

    @Override
     public int echo(String nodeIdp, String msg) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);   
        if (connectedNodes_.contains(nodeId)) {
            System.out.println(index+": "+msg);
            index++;
        }
      return index;
    }

    // ------------------ Private  -------------------
    private void addConnectedNode(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        if (!connectedNodes_.contains(nodeId)) {
            connectedNodes_.add(nodeId);
        }
    }
    
    // Hashset of node identities of the current nodes in the network.
    private HashSet<NodeIdentity> connectedNodes_;
    
    // Used for centralised mutual exclusion.
    // Guards connectedNodes_.
    private boolean networkUpdate_;
    
    // Queue of the nodes who wait to perform an update to the network nodes.
    // Guards connectedNodes_.
    private Queue<NodeIdentity> networkUpdateQueue_;
    
    // Used for testing only.
    private volatile int index = 1;

}
