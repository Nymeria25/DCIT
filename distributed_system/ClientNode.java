package distributed_system;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Lavinia
 */
public class ClientNode {

    public static void main(String[] args) {//throws Exception {

        Scanner sc = new Scanner(System.in);

        try {
            System.out.print("Server address and port : ");
            Socket clientSocket = new Socket(sc.next(), sc.nextInt()); //the
            // object takes a parameter for IP address and a port.
            DataOutputStream os = new DataOutputStream(clientSocket.
                    getOutputStream()); //the output stream of the
            //client socket.
            final DataInputStream is = new DataInputStream(clientSocket.
                    getInputStream()); //the input stream of the client socket.
            
            String message = sc.nextLine();
            os.writeUTF(message);

            String st = "";
            //boolean okay = false;

            Thread T = new Thread(new Runnable() {
                @Override
                public void run() { 
                    while (true) {
                        String s = "";
                        try {
                            s = is.readUTF();  
                            if (s.equals("$kill")) {
                                System.exit(0);
                            }
                            System.out.println(s);

                        } catch (IOException ex) {
                        }

                    }
                }
            });
            T.start(); 

            while (true) {
                st = sc.nextLine();
                if (!st.equals("")) 
                {
                    os.writeUTF(st);
                }
            }

        } catch (Exception e) {
            System.out.println("Connection failed!");
        }

    }
}
