package distributed_system_rpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

public class RpcClient {

    RpcClient(String IPAddress, int port, int serverPort) throws MalformedURLException {      
        connectionUpdaters_ = new ConcurrentHashMap<>();
        clientNodeId_ = new NodeIdentity(IPAddress, port);
        serverNodeId_ = new NodeIdentity(IPAddress, serverPort);
        
        ClientFactory factory = CreateClientFactory(serverNodeId_);
        myServer_ = (ConnectionUpdaterService) factory.
                newInstance(ConnectionUpdaterService.class);
        
        factory = CreateClientFactory(clientNodeId_);
        myClient_ = (ConnectionUpdaterService) factory.
                newInstance(ConnectionUpdaterService.class);
        
        // AddConnectionUpdater(clientNodeId_);
        AddConnectionUpdater(serverNodeId_); 
    }
    
    // Adds a new ConnectionUpdater object in the hashmap, for the specified
    // NodeIdentity.
    private boolean AddConnectionUpdater(NodeIdentity nodeId) throws
            MalformedURLException {
        if (!connectionUpdaters_.containsKey(nodeId)) {
        ClientFactory factory = CreateClientFactory(nodeId);
        UpdateConnectionUpdaters(nodeId, factory);
        return true;
        }
        return false;
    }
    
    private ClientFactory CreateClientFactory(NodeIdentity nodeId) throws
            MalformedURLException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setEnabledForExtensions(true);
        config.setServerURL(new URL("http", nodeId.getKey(), nodeId.getValue(),
                "xmlrpc"));
        
        // Creates an XmlRpcClient and instantiates a ClientFactory.
        XmlRpcClient rpcClient = new XmlRpcClient();
        rpcClient.setConfig(config);        
        return new ClientFactory(rpcClient);
    }
    
    public void UpdateNodeList() throws MalformedURLException {
        Vector<String> nodes = myClient_.getConnectedNodes();
        boolean updated = false;
        for (String node : nodes) {
            NodeIdentity nodeId = new NodeIdentity(node);
            if (AddConnectionUpdater(nodeId)) {
                updated = true;
            }
        }
        
        if (updated) {
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService>
                        cuEntry : connectionUpdaters_.entrySet()) {
                    cuEntry.getValue().join(
                            serverNodeId_.toString());
                }
        }
    }
    
    void RunClientConsole() throws XmlRpcException, MalformedURLException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Command: ");
            String command = scanner.next();
            
            if ("$echo".equals(command)) {
                String message = scanner.next();
                this.ClientEcho(message);
            }
            else if ("$join".equals(command)) {
                myClient_.join(serverNodeId_.toString());
                UpdateNodeList();
                // myServer_.join(serverNodeId_.toString());
              /*  for (Map.Entry<NodeIdentity, ConnectionUpdaterService>
                        cuEntry : connectionUpdaters_.entrySet()) {
                    cuEntry.getValue().join(
                            serverNodeId_.toString());
                } */
                
       
            } else if ("$print".equals(command)) {
                myServer_.print();
            }
                       
            else if ("$kill".equals(command)) {
                System.exit(0);
            }
            
        }
    }
    
        
    void ClientEcho(String message) throws XmlRpcException {
        for (Map.Entry<NodeIdentity, ConnectionUpdaterService>
                        cuEntry : connectionUpdaters_.entrySet()) {
            cuEntry.getValue().echo(cuEntry.getKey().toString(), message);
        }
    }
    
     private void UpdateConnectionUpdaters(NodeIdentity nodeId,
             ClientFactory factory) {
        if (!connectionUpdaters_.containsKey(nodeId)) {
            ConnectionUpdaterService cu = (ConnectionUpdaterService) factory.
                newInstance(ConnectionUpdaterService.class);
            // monkey business
           // cu.join(serverNodeId_.toString());
            connectionUpdaters_.put(nodeId, cu);
        }
    }
    
    
    
   // private final String IPAddress_;
   // private final int port_;
    // private final HashMap<Map.Entry<String, Integer>, XmlRpcClient> rpcClients_;
    // private final XmlRpcClient rpcClient_;
    private final ConcurrentHashMap<NodeIdentity, ConnectionUpdaterService>
            connectionUpdaters_;
    private final NodeIdentity clientNodeId_, serverNodeId_;
    ConnectionUpdaterService myServer_, myClient_;
}
