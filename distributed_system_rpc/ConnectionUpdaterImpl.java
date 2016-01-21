package distributed_system_rpc;

import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionUpdaterImpl implements ConnectionUpdaterService {

    public ConnectionUpdaterImpl(NodeIdentity nodeId) {
        nodeId_ = nodeId;
        network_ = new Network(nodeId);
        networkUpdateQueue_ = new ConcurrentLinkedQueue<>();
        sentenceUpdateQueue_ = new ConcurrentLinkedQueue<>();
        sentenceUpdate_ = false;
        networkUpdate_ = false;
        raMutex_ = false;
        iAmMaster_ = false;
        algorithm_ = "";
        sentence_ = "";
    }
    
    
    // If the critical zone with the list of nodes is not accessed by other node
    // at the moment of call, returns true.
    // Otherwise, blocks the caller until the critical zone with the list of
    // nodes is free again and then returns true.
    @Override
    public boolean performNetworkUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        networkUpdateQueue_.add(nodeId);
        
        // Block the client while other node is performing updates.
        while(!(networkUpdate_ == false && networkUpdateQueue_.peek() == nodeId)) {}
        
        // The critical zone of connected nodes is free.
        // That is, nodeId is on top of the queue and no one is updating the
        // network at this time (networkUpdate_ = false).
        networkUpdate_ = true;        
        return true;
    }

    @Override
    public boolean doneNetworkUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        if (nodeId.equals(networkUpdateQueue_.peek())) {
            networkUpdate_ = false;
            networkUpdateQueue_.poll();
            return true;
        }
        return false;
    }

    @Override
    public boolean setAsMaster() {
        iAmMaster_ = true;
        return true;
    }
    
    @Override
    public boolean isMaster() {
        return iAmMaster_;
    }
    
    private void PrintQueue() {
        for (NodeIdentity nodeId : sentenceUpdateQueue_) {
            System.out.println(nodeId.toString());
        }
    }
 
    @Override
    public boolean performSentenceUpdate(String nodeIdp) {
        System.out.println(nodeIdp + " wants access!");
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        sentenceUpdateQueue_.add(nodeId);
        System.out.println(sentenceUpdateQueue_.size());
        
        
        PrintQueue();
        
        // Block the client while other node is performing updates.
        while(!(sentenceUpdate_ == false && sentenceUpdateQueue_.peek() == nodeId)) {
            try {
                Thread.sleep(generateRandomWaitingTime(500, 800));
            } catch (InterruptedException ex) {
                Logger.getLogger(ConnectionUpdaterImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(sentenceUpdate_ + " " + nodeId.toString());
        }
        
        // The critical zone of connected nodes is free.
        // That is, nodeId is on top of the queue and no one is updating the
        // network at this time (sentenceUpdate_ = false).
        System.out.println(nodeIdp + " got access to update.");
        System.out.println(sentenceUpdateQueue_.size());
        sentenceUpdate_ = true;        
        return true;
    }


    @Override
    public boolean doneSentenceUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        while (sentenceUpdateQueue_.peek()!= null &&
                !nodeId.equals(sentenceUpdateQueue_.peek())) {}
            sentenceUpdate_ = false;
            sentenceUpdateQueue_.poll();
            System.out.println(nodeIdp + " released access to update.");
           // return true;
        
        return true;
    }
    
    
    @Override
    public boolean ricartAgrawalaReq(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        network_.getGrantedAccess(lamport_, nodeId);
       // while (!network_.hasGrantedAccess()) {}
        
        raMutex_ = true;
        return true;        
    }
    
    @Override
    public boolean doneRicartAgrawalaReq() {
        raMutex_ = false;
        return true;
    }
    
    public boolean getAccess(long lamport, String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        lamport_ = Math.max(lamport_, lamport) + 1;
        System.err.println("My lamport " + lamport_);
        System.err.println("Their lamport " + lamport);
        
        System.err.println("My node id " + nodeId_.toString());
        System.err.println("Their node id " + nodeIdp);
        System.err.println("Mutex state: " + raMutex_);
        while (! (raMutex_ == false && CompareExtendedLamport(lamport,
                nodeId) == -1)) {
            System.err.println("in while");
        }       
        return true;
    }
    
    // READ/WRITE impl
    @Override
    public boolean readWrite(String algorithm) {
        algorithm_ = algorithm;
            if (algorithm_.length() > 0 && !iAmMaster_) {
                List<String> appendedWords = ReadWriteImpl();

                System.out.println("Master's sentence: ");
                String masterString = network_.getSentenceFromMaster();
                System.out.println(masterString);
                
                int numNonAppended = 0;
                for (String word : appendedWords) {
                    if (!masterString.contains(word)) {
                        numNonAppended++;
                    }
                }
                
                System.out.println("Number of words not appended: " + numNonAppended);
                System.exit(0);
            }
        return false;
    }
    
    private int generateRandomWaitingTime(int Min, int Max) {
        return Min + (int) (Math.random() * ((Max - Min) + 1));
    }


    private List<String> ReadWriteImpl() {
        Dictionary dictionary = new Dictionary();

        int totalTime = 20000; // ms
        long startTime = System.currentTimeMillis();
        String clientSentence = "";

        List<String> appendedWords = new ArrayList<String>();
        while (System.currentTimeMillis() - startTime < totalTime) {
            try {
                Thread.sleep(generateRandomWaitingTime(100, 200));
                String word = dictionary.getRandomWord();
                System.out.println(word);
                appendedWords.add(word);
                
                if (algorithm_.equals("Centralized Mutual Exclusion")) {
                    network_.performSentenceUpdate(nodeId_);
                    clientSentence = network_.getSentenceFromMaster();
                    clientSentence += word;
                    network_.writeSentenceToMaster(clientSentence);
                    network_.doneSentenceUpdate(nodeId_);
                } else {
                    System.out.println("deep shit");
                    network_.ricartAgrawalaReq(nodeId_);
                    network_.writeSentenceToMaster(clientSentence);
                    network_.doneRicartAgrawalaReq();
                }
                
            } catch (Exception e) {

            }
        }
      return appendedWords;
    }
    
    // --------------

    
    @Override
    public boolean readWriteReady(String algorithm) {
            algorithm_ = algorithm;
            network_.notifyReadWrite(algorithm_);
   
        return true;
    }
   
    
    @Override
    public boolean startReadWrite(String algorithm) {
        algorithm_ = algorithm;
        network_.ElectMasterNode();
        return true;
    }
    
    
    @Override
    public String getReadWriteStatus() {
        return algorithm_;
    }
    
    
    @Override
    public String getSentence() {
        return sentence_;
    }
    /*
    @Override
    public String getSentenceFromMaster() {
        return network_.getSentenceFromMaster();
    }
    */
    
    @Override
    public boolean writeSentence(String sentence) {
        System.out.println("Writing: " + sentence);
        sentence_ = sentence;
        //System.out.println(nodeIdp + " released access to update.");
        return true;
    }
    /*
    @Override
    public boolean writeSentenceToMaster(String sentence) {
        network_.writeSentenceToMaster(sentence);
        return true;
    } */
   
    @Override
    public boolean joinNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        try {
            network_.joinNode(nodeId);
        } catch (MalformedURLException ex) {
            // do the konga
        }
        return true;
    }
    
    public boolean addNodeToNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        try {
            network_.AddConnectionUpdater(nodeId);
        } catch (MalformedURLException ex) {
            // do the konga
        }
        return true;
    }
    
    public boolean signOff(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        network_.signOff(nodeId);
        return true;
    }
    
    @Override
    public boolean removeNodeFromNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        network_.removeNode(nodeId);
        return true;
    }
    
    @Override
    public Vector<String> getConnectedNodes() {
        Vector<String> connected = network_.getNodes();
        return connected;
    }
    
    @Override
    public boolean print() {
        Vector<String> connected = network_.getNodes();
        System.out.println(connected.size());
        for (String node : connected) {
            System.out.println(node);
        }
        return true;
    }

    // ------------------ Private  -------------------
    
    private int CompareExtendedLamport(long lamport, NodeIdentity nodeId) {
        if (lamport_ == lamport) {
            return nodeId_.compareTo(nodeId);
        }
        
        if (lamport_ < lamport) return -1;
        if (lamport_ > lamport) return 1;
        
        return 0;
    }
    
    private String algorithm_;
    
    // Hashset of node identities of the current nodes in the network.
    private Network network_;
    
    // Used for centralised mutual exclusion.
    // Guards connectedNodes_.
    private boolean networkUpdate_, sentenceUpdate_;
    
    // Queue of the nodes who wait to perform an update to the network nodes.
    // Guards connectedNodes_.
    private ConcurrentLinkedQueue<NodeIdentity> networkUpdateQueue_, sentenceUpdateQueue_;
    
    // Ricart Agrawala
    private boolean raMutex_;
    private long lamport_;
    NodeIdentity nodeId_;
    
    private String sentence_;
    
    private boolean iAmMaster_;
 
    // Used for testing only.
    private volatile int index = 1;

}
