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
    
    @Override
    public boolean electMaster() {
        network_.ElectMasterNode();
        return true;
    }

    @Override
    public boolean setMaster(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        if (nodeId.equals(nodeId_)) {
            iAmMaster_ = true;
        }
        network_.setMaster(nodeId);
        return true;
    }

    @Override
    public boolean isMaster() {
        return iAmMaster_;
    }
    
    // ------------------ Read/write cycle ------------------
    
    // Syncronised read/write cycle.
    @Override
    public boolean readWrite(String algorithm) {
       // network_.ElectMasterNode();

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

    // CME
    // Acquires lock over the master sentence.
    @Override
    public boolean performSentenceUpdate(String nodeIdp) {
        System.out.println(nodeIdp + " wants access!");
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        sentenceUpdateQueue_.add(nodeId);
        System.out.println(sentenceUpdateQueue_.size());

        PrintQueue();

        // Blocks the client while other node is performing updates.
        while (!(sentenceUpdate_ == false && sentenceUpdateQueue_.peek() == nodeId)) {
            try {
                Thread.sleep(generateRandomWaitingTime(500, 800));
            } catch (InterruptedException ex) {
                // nothing
            }
            System.out.println(sentenceUpdate_ + " " + nodeId.toString());
        }

        // The critical zone is free.
        // That is, nodeId is on top of the queue and no one is updating the
        // sentence at this time (sentenceUpdate_ = false).
        System.out.println(nodeIdp + " got access to update.");
        System.out.println(sentenceUpdateQueue_.size());
        sentenceUpdate_ = true;
        return true;
    }

    @Override
    public boolean doneSentenceUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        while (!nodeId.equals(sentenceUpdateQueue_.peek())) {
        }
        sentenceUpdate_ = false;
        sentenceUpdateQueue_.poll();
        System.out.println(nodeIdp + " released access to update.");

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
        while (!(raMutex_ == false && CompareExtendedLamport(lamport,
                nodeId) == -1)) {
            System.err.println("in while");
        }
        return true;
    }

    

    private int generateRandomWaitingTime(int Min, int Max) {
        return Min + (int) (Math.random() * ((Max - Min) + 1));
    }

   
    // --------------
    @Override
    public boolean readWriteReady(String algorithm) {
        algorithm_ = algorithm;
        network_.notifyReadWrite(algorithm_);
        return true;
    }

    @Override
    public String getReadWriteStatus() {
        return algorithm_;
    }

    @Override
    public String getMasterSentence() {
        return sentence_;
    }

    @Override
    public boolean writeMasterSentence(String sentence) {
        System.out.println("Writing: " + sentence);
        sentence_ = sentence;
        //System.out.println(nodeIdp + " released access to update.");
        return true;
    }

    // ------------------ Network builder methods ------------------
    
    // CME
    // Acquires lock over the list of nodes in the network.
    @Override
    public boolean performNetworkUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        networkUpdateQueue_.add(nodeId);

        // Block the client while other node is performing updates.
        while (!(networkUpdate_ == false && networkUpdateQueue_.peek() == nodeId)) {}

        // The critical zone of connected nodes is free.
        // That is, nodeId is on top of the queue and no one is updating the
        // network at this time (networkUpdate_ = false).
        networkUpdate_ = true;
        return true;
    }

    // CME
    // Releases the lock over the list of nodes in the network.
    @Override
    public boolean doneNetworkUpdate(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        while (!nodeId.equals(networkUpdateQueue_.peek())) {}
        networkUpdate_ = false;
        networkUpdateQueue_.poll();
        return true;

    }
    
    @Override
    public boolean joinNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        try {
            network_.joinNode(nodeId);
        } catch (MalformedURLException ex) {
            // nothing
        }
        return true;
    }

    @Override
    // Adds nodeIdp to the list of connected nodes of each node in the network.
    public boolean addNodeToNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        try {
            network_.AddConnectionUpdater(nodeId);
        } catch (MalformedURLException ex) {
            // nothing
        }
        return true;
    }

    @Override
    // Notifies all the nodes in the network to remove nodeIdp from their list.
    public boolean signOff(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        network_.signOff(nodeId);
        return true;
    }

    @Override
    // Removes nodeIdp from the list of connected nodes.
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
    public boolean isAlive() {
        return true;
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
                    network_.ricartAgrawalaReq(nodeId_);
                    network_.performSentenceUpdate(nodeId_);
                    clientSentence = network_.getSentenceFromMaster();
                    clientSentence += word;
                    network_.writeSentenceToMaster(clientSentence);
                    network_.doneRicartAgrawalaReq();
                }

            } catch (Exception e) {

            }
        }
        return appendedWords;
    }

     // ------------------ Helper methods ------------------
    private void PrintQueue() {
        for (NodeIdentity nodeId : sentenceUpdateQueue_) {
            System.out.println(nodeId.toString());
        }
    }


    private int CompareExtendedLamport(long lamport, NodeIdentity nodeId) {
        if (lamport_ == lamport) {
            return nodeId_.compareTo(nodeId);
        } else if (lamport_ < lamport) {
            return -1;
        } else if (lamport_ > lamport) {
            return 1;
        }
        return 0;
    }

    // The algorithm to be used for mutual exclusion. (CME or Ricart Agrawala)
    private String algorithm_;

    // CME
    // Guards network_.
    private boolean networkUpdate_;
    
    // CME waiting queue for network update.
    private ConcurrentLinkedQueue<NodeIdentity> networkUpdateQueue_;
    
    // Network handling the underlying calls to the other connection updaters
    // in the network.
    private Network network_;
    
    // CME
    // Guards sentence_.
    private boolean sentenceUpdate_;
    
    // CME waiting queue for master sentence update.
    private ConcurrentLinkedQueue<NodeIdentity> sentenceUpdateQueue_;
    
    private String sentence_;

    // Ricart Agrawala
    private boolean raMutex_;
    private long lamport_;
    NodeIdentity nodeId_;

    private boolean iAmMaster_;

    // Used for testing only.
    private volatile int index = 1;

}
