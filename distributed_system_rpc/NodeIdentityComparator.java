package distributed_system_rpc;

// Compares two NodeIdentity objects in reverse order.

import java.util.Comparator;

public class NodeIdentityComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        NodeIdentity nodeId1 = (NodeIdentity) o1;
        NodeIdentity nodeId2 = (NodeIdentity) o2;
       
        return nodeId1.compareTo(nodeId2) * (-1);
    }
}
