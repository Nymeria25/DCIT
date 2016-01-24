/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributed_system_rpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.Scanner;
import org.apache.xmlrpc.XmlRpcException;

/**
 *
 * @author Lavinia
 */
public class NetworkNodeMain {

    public static void main(String[] args) throws IOException, XmlRpcException,
            MalformedURLException, InterruptedException {

        System.out.print("Run on port: ");
        Scanner scanner = new Scanner(System.in);
        int serverPort = scanner.nextInt();
        NodeIdentity serverId = new NodeIdentity(InetAddress.getLocalHost().
                getHostAddress(), serverPort);

        RpcServer rpcServer = new RpcServer(serverId);
        rpcServer.startServer();

        System.out.print("Connect to IP address and port: ");
        String primaryServerIpAddress = scanner.next();
        int primaryServerPort = scanner.nextInt();

        
        NodeIdentity primaryServerId = new NodeIdentity(primaryServerIpAddress,
                primaryServerPort);
        final RpcClient rpcClient = new RpcClient(serverId, primaryServerId);

 /*        
        Thread t2 = new Thread() {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(generateRandomNumber(1000, 2000));
                      //  rpcClient.StartReadWrite();
                        rpcClient.masterJob();
                    }
                } catch (InterruptedException ex) {
                    System.err.println("Interrupted thread.");
                }
            }
        };
        t2.start(); */

        rpcClient.RunClientConsole();
    }

    static int generateRandomNumber(int Min, int Max) {
        return Min + (int) (Math.random() * ((Max - Min) + 1));
    }
};
