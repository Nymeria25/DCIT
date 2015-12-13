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
    private final String key;
    private int value;

    public NodeIdentity(String key, Integer value) {
        this.key = key;
        this.value = value;
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


