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
     public int echo(String nodeIdp, String msg) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        
        if (connectedNodes_.contains(nodeId)) {
            System.out.println(index+": "+msg);
            index++;
        }
      System.out.println(index+": "+msg);
      index++;
      return index;
    }
    
    @Override
    public Vector<String> join(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        if (!connectedNodes_.contains(nodeId)) {
            connectedNodes_.add(nodeId);
        }
        return getConnectedNodes();
    }
    
    private Vector<String> getConnectedNodes() {
        Vector<String> connected = new Vector<>();
        for (NodeIdentity nodeId : connectedNodes_) {
            connected.add(nodeId.toString());
        }
        return connected;
    }
    
 
    
}
