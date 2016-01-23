/*
 * The common interface between clients and servers.
 */
package distributed_system_rpc;

import java.util.Vector;

public interface ConnectionUpdaterService {
    
    // ------------------ Centralised Mutual Exclusion -------------------
    // The client needs to call these handlers when they want to update the
    // network. (For joinNetwork() and removeFromNetwork()).
    // Example:
    //
    // connectionUpdater.performNetworkUpdate();
    // connectionUpdater.removeFromNetwork(nodeId);
    // connectionUpdater.doneNetworkUpdate();
    
    // Called BEFORE performing an update to the network nodes.
    // nodeIdp is the node identity of the node who want to perform a network
    // update. nodeIdp is the String identity of the node who calls. 
    // Should be host:port.
    public boolean performNetworkUpdate(String nodeIdp);
    
    // Called AFTER the client finished the update of the network. 
    // nodeIdp is the String identity of the node who calls. 
    // Should be host:port.
    public boolean doneNetworkUpdate(String nodeIdp);
    
    
    // Called BEFORE updating the master sentence. Acquires lock over sentence.
    // nodeIdp is the String identity of the node who calls. 
    // Should be host:port.
    public boolean performSentenceUpdate(String nodeIdp);
    
    // Called AFTER the client finished the update of the sentence. Releases
    // lock. nodeIdp is the String identity of the node who calls. 
    // Should be host:port.
    public boolean doneSentenceUpdate(String nodeIdp);
    
    
   
    public boolean getAccess(long lamport, String nodeIdp);
    // Got an OK from nodeIdp.
    public boolean OK(String nodeIdp);
    
    public boolean setMaster(String nodeIdp);
    public boolean electMaster();
    public boolean isMaster();
    
    
    // ------------------ Operation Handlers -------------------
    
    // Returns a vector of host:port Strings, representing the node identities
    // of the nodes currently in the network.
    public Vector<String> getConnectedNodes();
    
    // Called by a new node who wants to join the network.
    // Broadcasts the operation to the other existing nodes in the network, to
    // form a full mesh.
    public boolean joinNetwork(String nodeIdp);
    
    // Adds nodeIdp to the current node's list of connected nodes.
    public boolean addNodeToNetwork(String nodeIdp);
    
    // Called by a node who wants to sign off from the network.
    // Broadcasts the operation to the other existing nodes in the network, to
    // update their lists of nodes.
    public boolean signOff(String nodeIdp);
    
    // Removes nodeIdp from the list of connected nodes of the current node.
    public boolean removeNodeFromNetwork(String nodeIdp);
    
    // Called by the node who starts the read/write process. Broadcasts the 
    // operation to the other existing nodes in the network, running the
    // readWrite() method in separate threads, for all the nodes.
    public boolean readWriteReady(String algorithm);
    
    // The actual read/write process.
    public boolean readWrite(String algorithm);
    

    
    // Returns the name of the algorithm to be performed for read/write.
    // "Centralized Mutual Exclusion" or "Ricart Agrawala".
    // If the return String is empty, it means the read/write operation
    // should not start yet.
   public String getReadWriteStatus();
    
    //
    public String getMasterSentence();
   // public String getSentenceFromMaster();
    
    //
    public boolean writeMasterSentence(String sentence);
  //  public boolean writeSentenceToMaster(String sentence);
    
    // ------------------ Helpers -------------------
    
    // Returns true. Used to ping nodes, to check if they are alive.
    public boolean isAlive();
     
    // Prints to output stream the list of nodes currently connected to the
    // network.
    public boolean print();
  }
