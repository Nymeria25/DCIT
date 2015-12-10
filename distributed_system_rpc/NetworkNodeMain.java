/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributed_system_rpc;

import java.io.IOException;
import java.util.Scanner;
import org.apache.xmlrpc.XmlRpcException;

/**
 *
 * @author Lavinia
 */
public class NetworkNodeMain {

    public static void main(String[] args) throws IOException, XmlRpcException {
        
        System.out.print("Run on port: ");
        Scanner scanner = new Scanner(System.in); 
        int port = scanner.nextInt();
        
        RpcServer rpcServer = new RpcServer(port);
        rpcServer.startServer();
        
        System.out.print("Connect to IP address and port: ");
        String IpAdress = scanner.next();
        port = scanner.nextInt();
        RpcClient rpcClient = new RpcClient(IpAdress, port);
        
        // for each node we connect to, we need to create a new XmlRpcClient.
        // so that we can invoke a method on any of the other nodes?
        
        rpcClient.RunClientConsole();
        
        
    }
    
}
