/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import distributed_system_rpc.ConnectionUpdaterImpl;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Lavinia
 */
public class ConnectionUpdaterTest {
    private static ConnectionUpdaterImpl cu_;
    
    public ConnectionUpdaterTest() {
        cu_ = new ConnectionUpdaterImpl();
    }
    
    @Test
    public void JoinIsOk() {
        cu_.join("1:123");
        cu_.join("2:123");
        cu_.join("1:123");
        
        Vector<String> nodes = cu_.getConnectedNodes();
        
        assertTrue(nodes.contains("1:123"));
        assertTrue(nodes.contains("2:123"));
        assertEquals(2, nodes.size());
    }
    
    @Test
    public void SignOffFromEmptyListIsOk() {
        cu_.signOff("1:123");
        cu_.signOff("2:123");
        cu_.signOff("1:123");
        
        Vector<String> nodes = cu_.getConnectedNodes();
        assertEquals(0, nodes.size());
    }
    
    @Test
    public void JoinAndSignOffWithoutRepetitionsIsOk() {
        cu_.join("1:123");
        cu_.join("2:123");
        cu_.signOff("2:123");
        
        Vector<String> nodes = cu_.getConnectedNodes();
        assertTrue(nodes.contains("1:123"));
        assertEquals(1, nodes.size());
        
        cu_.join("2:123");
        cu_.join("3:11");
        nodes = cu_.getConnectedNodes();
        assertTrue(nodes.contains("1:123"));
        assertTrue(nodes.contains("2:123"));
        assertTrue(nodes.contains("3:11"));
        assertEquals(3, nodes.size());
    }
    
}
