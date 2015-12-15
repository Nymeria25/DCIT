package distributed_system_rpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

public class RpcClient {

    RpcClient(String IPAddress, int port) throws MalformedURLException {
        
        NodeIdentity nodeId = new NodeIdentity(IPAddress, port);
        ClientFactory factory = CreateClientFactory(nodeId);
        
        connectionUpdaters_ = new HashMap<>();
        UpdateConnectionUpdaters(nodeId, factory);
        
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
                HashMap<String, Set<Integer>> newNodes = new HashMap<>();
                System.out.println(newNodes.toString());
              /*  for (Map.Entry<NodeIdentity, ConnectionUpdaterService>
                        cuEntry : connectionUpdaters_.entrySet()) {
                    HashMap<NodeIdentity, ConnectionUpdaterService> nodes = cuEntry.getValue().
                            join((Object) cuEntry.getKey());
                    newNodes.putAll(nodes);
                }
                CreateXmlRpcClients(newNodes);  */
                
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
            connectionUpdaters_.put(nodeId, cu);
        }
    }
    
    
    
   // private final String IPAddress_;
   // private final int port_;
    // private final HashMap<Map.Entry<String, Integer>, XmlRpcClient> rpcClients_;
    // private final XmlRpcClient rpcClient_;
    private final HashMap<NodeIdentity, ConnectionUpdaterService>
            connectionUpdaters_;
}
