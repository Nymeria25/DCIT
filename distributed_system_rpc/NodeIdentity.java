package distributed_system_rpc;

import java.util.Map;
import java.util.Objects;


// Stores the IPAddress and port for a node.
 public class NodeIdentity implements Map.Entry<String, Integer>, 
         Comparable<NodeIdentity> {
     
    private String key;
    private int value;

    public NodeIdentity(String key, Integer value) {
        this.key = key;
        this.value = value;
    }
    
    // nodeId has the form IPAddress:port.
    public NodeIdentity(String nodeId) {
        String delims = "[:]";
        String[] tokens = nodeId.split(delims);
        
        try {
            this.key = tokens[0];
            this.value = Integer.parseInt(tokens[1]); 
        } catch(IndexOutOfBoundsException e) {
            // TODO: maybe log error
            System.err.println("IndexOutOfBoundsException for NodeIdentity"
                    + " parsing!" + e.getMessage());
            this.key = " ";
            this.value = 0;
        }
        
    }
    
    @Override
    public String toString() {
        return key + ":" + String.valueOf(value);
    }
    
    @Override
    public boolean equals(Object nodeIdp) {
        if((nodeIdp == null) || (getClass() != nodeIdp.getClass())){
        return false;
    } 
        NodeIdentity nodeId = (NodeIdentity) nodeIdp;
        return (key.equals(nodeId.getKey())) && (value == nodeId.getValue());
    }
    
    @Override
    public int compareTo(NodeIdentity ni) {
        return this.toString().compareTo(ni.toString());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.key);
        hash = 79 * hash + this.value;
        return hash;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public Integer setValue(Integer value) {
        Integer old = this.value;
        this.value = value;
        return old;
    }

    }


