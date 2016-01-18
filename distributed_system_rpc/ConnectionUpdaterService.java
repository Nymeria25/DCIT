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
    
    // Called BEFORE performing an update to the network nodes. That is,
    // in the case when a node joins, signs off, or fails.
    // nodeIdp is the node identity of the node who want to perform a network
    // update. nodeIdp should be host:port.
    public boolean performNetworkUpdate(String nodeIdp);
    
    // Called AFTER the client finished the update of the network. Also,
    // in the case when a node joins, signs off, or fails.
    public boolean doneNetworkUpdate(String nodeIdp);
    
    
    
    public boolean performSentenceUpdate(String nodeIdp);
    
    // Called AFTER the client finished the update of the network. Also,
    // in the case when a node joins, signs off, or fails.
    public boolean doneSentenceUpdate(String nodeIdp);
    
    
    
    public boolean ricartAgrawalaReq(String nodeIdp);
    public boolean doneRicartAgrawalaReq();
    public boolean getAccess(long lamport, String nodeIdp);
    
    public boolean setAsMaster();
    public boolean isMaster();
    
    
    // ------------------ Operation Handlers -------------------
    
    // Returns a vector of host:port Strings, representing the node identities
    // of the nodes currently in the network.
    public Vector<String> getConnectedNodes();
    
    // DOES STUFF
    public boolean joinNetwork(String nodeIdp);
    public boolean addNodeToNetwork(String nodeIdp);
    
    // MORE STUFF
    public boolean signOff(String nodeIdp);
    public boolean removeNodeFromNetwork(String nodeIdp);
    
    //
    public boolean readWriteReady(String algorithm);
    public boolean startReadWrite(String algorithm);
    
    // Returns the name of the algorithm to be performed for read/write.
    // "Centralized Mutual Exclusion" or "Ricart Agrawala".
    // If the return String is empty, it means the read/write operation
    // should not start yet.
    public String getReadWriteStatus();
    
    //
    public String getSentence();
    public String getSentenceFromMaster();
    
    //
    public boolean writeSentence(String sentence);
    public boolean writeSentenceToMaster(String sentence);
    
    // ------------------ Testing -------------------
    
    // Echoes the message sent in the request.
    // nodeIdp is the node identity of the sender and should be in the form of
    // host:port.
    // msg is the message sent.
   // public int echo(String nodeIdp, String msg);
     
    // Prints to output stream the list of nodes currently connected to the
    // network.
    public boolean print();
  }
