/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributed_system_rpc;


public interface ConnectionUpdaterService {
    // Updates the server's list of nodes.
    // Takes the IPAddress and pot of the new node.
    public String join(String IPAddress, int port);
    
    // Echoes the message sent in the request.
    // IPAddress is the IP address of the client who performs the request,
    // running on port port.
    // msg is the message sent.
    // Only used for testing.
    public int echo(String IPAddress, int port, String msg);
  }
