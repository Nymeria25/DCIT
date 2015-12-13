package distributed_system_rpc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConnectionUpdaterImpl implements ConnectionUpdaterService {

    // HashMap with IPAddress as key and port as value.
    private HashSet<NodeIdentity> connectedNodes_;
    private volatile int index = 1;
    
    
    public ConnectionUpdaterImpl() {
        connectedNodes_ = new HashSet<>();
    }

    @Override
    public int echo(NodeIdentity nodeId, String msg) {
        if (connectedNodes_.contains(nodeId)) {
            System.out.println(index+": "+msg);
            index++;
        } 
      return index;
    }
    
    @Override
    public HashSet<NodeIdentity> join(NodeIdentity nodeId) {
        if (!connectedNodes_.contains(nodeId)) {
            connectedNodes_.add(nodeId);
        }
        return connectedNodes_;
    }
    
 
    
}
