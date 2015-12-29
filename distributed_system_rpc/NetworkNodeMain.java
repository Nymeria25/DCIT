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

       
        // maybe remove timer and leave this run sequencially in a thread here?
         TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    rpcClient.UpdateNodeList();
                } catch (MalformedURLException ex) {
                    Logger.getLogger(NetworkNodeMain.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        Timer timer = new Timer("WhatAHorribleIdea");//create a new Timer
        timer.scheduleAtFixedRate(timerTask, 30, 30);
        
        rpcClient.RunClientConsole();
    }
};
