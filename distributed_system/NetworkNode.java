package distributed_system;

import distributed_system.NodeConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
/**
 *
 * @author Lavinia
 */
public class NetworkNode {
    
    private ArrayList<DataOutputStream> outputStreams_;
    private ArrayList<DataInputStream> inputStreams_;
    private ServerSocket serverSocket_;
    private ArrayList<Socket> clientSockets_;
    
    //this constructs the object
    public NetworkNode() {
        outputStreams_ = new ArrayList<>();
        inputStreams_ = new ArrayList<>();
        clientSockets_= new ArrayList<>();
        serverSocket_ = null;
    }
    void InstantiateNode() throws IOException {
    
        Scanner sc = new Scanner(System.in);
    System.out.print("Run on : ");
    serverSocket_ = new ServerSocket( sc.nextInt() );  // Instantiate the ServerSocket with
    // the port read from the keyboard.
    System.out.println("The server is on!");
    
    //thread that performs commands
        Thread T = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String message = "";
                    try {
                        //collect the input data from all nodes
                        for( DataInputStream inputStream : inputStreams_){
                            message = inputStream.readUTF();
                        if (message.equals("$kill")) {
                            System.exit(0);
                        }
                        }
                        
                       // System.out.println(message);
                    } catch (IOException ex) {
                    }

                }
            }
        });
        T.start();
    while (true) { // Block the main thread to wait for clients.
        System.out.println("Command : ");
      Socket currentClientSocket = serverSocket_.accept(); // The server accepts
      // a new connection.
      outputStreams_.add(new DataOutputStream(currentClientSocket.
              getOutputStream())); // Store the outputstream for this socket.
      System.out.println("\nNew client. ");
      NodeConnection newClient = new NodeConnection(currentClientSocket, 
              outputStreams_); // Create a new connection for the newly connected
      // client.
      newClient.StartConnection();
    }
    }
}
