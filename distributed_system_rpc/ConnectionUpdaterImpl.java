package distributed_system_rpc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class ConnectionUpdaterImpl implements ConnectionUpdaterService {

    // TODO: explain
    private HashSet<NodeIdentity> connectedNodes_;
    private volatile int index = 1;
    
    public ConnectionUpdaterImpl() {
        connectedNodes_ = new HashSet<>();
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
    
    @Override
    public boolean join(String nodeIdp) {
        addConnectedNode(nodeIdp);
        return true;
    }
    
    @Override
    public boolean signOff(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        connectedNodes_.remove(nodeId);
        return true;
    }
    
    
    private void addConnectedNode(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        if (!connectedNodes_.contains(nodeId)) {
            connectedNodes_.add(nodeId);
        }
    }
    
    @Override
    public Vector<String> getConnectedNodes() {
        Vector<String> connected = new Vector<>();
        for (NodeIdentity nodeId : connectedNodes_) {
            connected.add(nodeId.toString());
        }
        return connected;
    }
    
 
    
}
