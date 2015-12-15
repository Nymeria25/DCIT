/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributed_system_rpc;

import java.util.Map;

/**
 *
 * @author Lavinia
 */
// Stores the IPAddress and port for a node.
    public class NodeIdentity implements Map.Entry<String, Integer> {
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
            this.key = "";
            this.value = 0;
        }
        
    }
    
    @Override
    public String toString() {
        return key + ":" + String.valueOf(value);
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


