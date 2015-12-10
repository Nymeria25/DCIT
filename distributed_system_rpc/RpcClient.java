package distributed_system_rpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

public class RpcClient {

    RpcClient(String IPAddress, int port) throws MalformedURLException {
        IPAddress_ = IPAddress;
        port_ = port;
        
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setEnabledForExtensions(true);
        config.setServerURL(new URL("http", IPAddress, port, "xmlrpc"));
        rpcClient_ = new XmlRpcClient();
        rpcClient_.setConfig(config);
        
        ClientFactory factory = new ClientFactory(rpcClient_);
        connectionUpdater_ = (ConnectionUpdaterService) factory.
                newInstance(ConnectionUpdaterService.class);
    }
    
    void RunClientConsole() throws XmlRpcException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Command: ");
            String command = scanner.next();
            
            if ("$echo".equals(command)) {
                String message = scanner.next();
                this.ClientEcho(message);
            } else if ("$kill".equals(command)) {
                System.exit(0);
            }
            
        }
    }
    
    void ClientEcho(String message) throws XmlRpcException {
        connectionUpdater_.echo(IPAddress_, port_, message);    
        
       /* Object[] params = new Object[]{new Integer(33), new Integer(9)};
    Integer result = (Integer) rpcClient_.execute("ConnectionUpdaterService.AddNumbers", params); 
    return result; */
    }
    
    private final String IPAddress_;
    private final int port_;
    private final XmlRpcClient rpcClient_;
    private final ConnectionUpdaterService connectionUpdater_;
}
