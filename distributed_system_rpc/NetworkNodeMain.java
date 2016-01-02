/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributed_system_rpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
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

        RpcServer rpcServer = new RpcServer(serverPort);
        rpcServer.startServer();

        System.out.print("Connect to IP address and port: ");
        String primaryServerIpAddress = scanner.next();
        int primaryServerPort = scanner.nextInt();

        NodeIdentity serverId = new NodeIdentity(InetAddress.getLocalHost().
                getHostAddress(), serverPort);
        NodeIdentity primaryServerId = new NodeIdentity(primaryServerIpAddress,
                primaryServerPort);
        final RpcClient rpcClient = new RpcClient(serverId, primaryServerId);

        Thread t = new Thread() {
            public void run() {
                try {
                    while(true) {
                   // System.out.println("in za thread");
                    Thread.sleep(generateRandomNumber(500,1000));
                    rpcClient.UpdateNetwork();
                    }
                } catch (MalformedURLException ex) {
                    Logger.getLogger(NetworkNodeMain.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(NetworkNodeMain.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        t.start();

        rpcClient.RunClientConsole();
    }
    
    static int generateRandomNumber(int Min, int Max) {
        return Min + (int)(Math.random() * ((Max - Min) + 1));
    }
};
