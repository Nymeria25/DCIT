package distributed_system;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Lavinia
 */
public class NodeConnection extends Thread {

    public NodeConnection(Socket clientSocketP, ArrayList<DataOutputStream> 
            outputStreamsP) throws IOException {
        clientSocket_ = clientSocketP;
        if (clientSocketP != null) {
            inputStream_ = new DataInputStream(clientSocket_.getInputStream());
            // Input stream to get messages from the "server".
            outputStream_ = new DataOutputStream(clientSocket_.getOutputStream());
            // Output stream to send messages to the "server".
            connectedOutputStreams_ = outputStreamsP;  // The list of all the other
            // connected nodes outputstreams.
        }
    }

    public void StartConnection() throws IOException {
        start(); // starts the thread
        // PUT THE CODE OF THE CLIENT HERE?
        String st;
        Scanner sc = new Scanner(System.in);
        while (true) {
                st = sc.nextLine();
                if (!st.equals("")) 
                {
                    System.out.print("Client stuff: ");
                    outputStream_.writeUTF(st);
                }
            }
    }
    
    @Override
    public void run() { // Method of "Thread".
        // This is run when the thread starts.
        while(true){
            try {
                String message = inputStream_.readUTF();
                System.out.println("Type: ");
                if (!message.equals("")){
                outputStream_.writeUTF(message + "+" +message);}
            } catch (IOException ex) {
                Logger.getLogger(NodeConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }
     
    }

    private Socket clientSocket_;
    private DataInputStream inputStream_;
    private DataOutputStream outputStream_;
    private ArrayList<DataOutputStream> connectedOutputStreams_;
}
