package distributed_system_rpc;

import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionUpdaterImpl implements ConnectionUpdaterService {

    public ConnectionUpdaterImpl(NodeIdentity nodeId) {
        nodeId_ = nodeId;
        network_ = new Network(nodeId);
        networkUpdateQueue_ = new ConcurrentLinkedQueue<>();
        sentenceUpdateQueue_ = new ConcurrentLinkedQueue<>();
        okSet_ = Collections.synchronizedSet(new HashSet<NodeIdentity>());
        replyQueue_ = new ConcurrentLinkedQueue<>();
        sentenceUpdate_ = false;
        networkUpdate_ = false;
        raSemaphore_ = "REALEASED";
        lamport_ = new AtomicInteger(0);
        iAmMaster_ = false;
        algorithm_ = "";
        sentence_ = "";
        sentenceHistory_ = new ConcurrentSkipListMap<>();
        networkSize_ = 0;
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
        sentence_ = "";
        lamport_.set(0);
        replyQueue_.clear();
        sentenceHistory_.clear();
        sentenceUpdateQueue_.clear();
        
        network_.setMaster(nodeId);
        return true;
    }

    @Override
    public boolean isMaster() {
        return iAmMaster_;
    }
    
    @Override
    public String getMaster() {
        return network_.getMasterId();
    }
    
    // ------------------ Read/write cycle ------------------
    
    // Syncronised read/write cycle.
    @Override
    public boolean readWrite(String algorithm) {

        networkSize_ = network_.getSize();
        algorithm_ = algorithm;
        if (algorithm_.length() > 0 && !iAmMaster_) {
            List<String> appendedWords = ReadWriteImpl();

            System.out.println("Master's sentence: ");
            String masterString = network_.getSentenceFromMaster();
            System.out.println(masterString);
            
            Vector<String> masterAppendedWords = network_.getSentenceUpdateHistory(nodeId_);

            System.out.println("Words successfully appended to master: ");
            for (String word : masterAppendedWords) {
                System.out.println(word);
            }
            
            int numNonAppended = 0;
            for (String word : appendedWords) {
                if (!masterAppendedWords.contains(word)) {
                    numNonAppended++;
                    System.out.println(word + " was not appended!");
                }
            }

            System.out.println("Number of words not appended: " + numNonAppended);
            /*
            try {
                // To make sure everyone is done.
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ConnectionUpdaterImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(0);
            */
        }
        return false;
    }

    // ------------------ CME ------------------
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

    // ------------------ RA ------------------
    public boolean ricartAgrawalaReq() {
        System.out.println("Want permission.");
        raSemaphore_ = "WANTED";
       // lamport_.incrementAndGet();
        int newLamport = network_.ricartAgrawalaReq(lamport_.get(), nodeId_.toString());
        lamport_.set(newLamport);
        
        // Block the request until the caller got N-1 OKs.
        while (okSet_.size() < networkSize_ - 1) {
            try {
                Thread.sleep(generateRandomWaitingTime(200, 1000));
                System.out.println("Waiting for permission");
                System.out.println("okCounter = " + okSet_.size());
                System.out.println("networkSize = " + networkSize_);
            } catch (InterruptedException ex) {
            }
        }
        
        // Got OK from all the other nodes, acquire lock.
        System.out.println("Got lock!");
        okSet_.clear();
        raSemaphore_ = "HELD";
        return true;
    }
    
    
    public boolean doneRicartAgrawalaReq() {
        // Releases lock.
        raSemaphore_ = "REALEASED";
        System.out.println("Released lock!");
        
        // Send OK to all the queued nodes.
        for (NodeIdentity nodeId : replyQueue_) {
            System.out.println("Sending ok to " + nodeId.toString());
            network_.sendOK(nodeId);
        }
        replyQueue_.clear();
        return true;
    }


    // The main logic of Ricart Agrawala.
    // Queues the caller iff the lock is held, or in case the lock is wanted,
    // if this extended lamport clock is smaller than the caller's lamport.
    @Override
    public boolean getAccess(int lamport, String nodeIdp) {
        lamport_.set(Math.max(lamport, lamport_.get()) + 1);
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        
        if ("HELD".equals(raSemaphore_) || ("WANTED".equals(raSemaphore_) && 
                CompareExtendedLamport(lamport, nodeId) < 0)) {
            replyQueue_.add(nodeId);
            System.out.println("added to queue");
        } else {
            network_.sendOK(nodeId);
            System.out.println(nodeId_.toString() + " Sent ok!");
        }
        
        System.out.println("my lamport " + lamport_.get());
        System.out.println("their lamport " + lamport);
        System.out.println("comparison: " + CompareExtendedLamport(lamport, nodeId));
        
        return true;
    }
    
    // Got an OK message from nodeIdp. Updates the set of received OKs.
    @Override
    public boolean OK(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        okSet_.add(nodeId);
        return true;
    }


    private int generateRandomWaitingTime(int Min, int Max) {
        return Min + (int) (Math.random() * ((Max - Min) + 1));
    }

   
    // --------------
    @Override
    public boolean start(String algorithm) {
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
    public Vector<String> getSentenceUpdateHistory(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        return sentenceHistory_.get(nodeId);
    }

    @Override
    public boolean writeMasterSentence(String nodeIdp, String sentence) {
        System.out.println("Writing: " + sentence);
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        if (sentence_.length() < sentence.length()) {
        String word = sentence.substring(sentence_.length(), sentence.length());
        UpdateSentenceHistory(nodeId, word);
        }
        sentence_ = sentence;
        return true;
    }
    
    private void UpdateSentenceHistory(NodeIdentity nodeId, String word) {
        if (sentenceHistory_.containsKey(nodeId)) {
            Vector<String> entries = sentenceHistory_.get(nodeId);
            entries.add(word);
            sentenceHistory_.put(nodeId, entries);
        } else {
            Vector<String> entries = new Vector<>();
            entries.add(word);
            sentenceHistory_.put(nodeId, entries);
        }
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

    // Adds nodeIdp to the list of connected nodes of each node in the network.
    @Override
    public boolean addNodeToNetwork(String nodeIdp) {
        NodeIdentity nodeId = new NodeIdentity(nodeIdp);
        try {
            network_.AddConnectionUpdater(nodeId);
        } catch (MalformedURLException ex) {
            // nothing
        }
        return true;
    }

    // Notifies all the nodes in the network to remove nodeIdp from their list.
    @Override
    public boolean signOff() {
        network_.signOff(nodeId_);
        return true;
    }
    
    // Removes nodeIdp from the list of connected nodes.
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
                Thread.sleep(generateRandomWaitingTime(1000, 5000));
                String word = dictionary.getRandomWord();
                System.out.println(word);
                appendedWords.add(word);

                if (algorithm_.equals("CME")) {
                    network_.performSentenceUpdate(nodeId_);
                    clientSentence = network_.getSentenceFromMaster();
                    clientSentence += word;
                    network_.writeSentenceToMaster(clientSentence);
                    network_.doneSentenceUpdate(nodeId_);
                } else {
                    ricartAgrawalaReq();
                    clientSentence = network_.getSentenceFromMaster();
                    clientSentence += word;
                    network_.writeSentenceToMaster(clientSentence);
                    doneRicartAgrawalaReq();
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
        if (lamport_.get() == lamport) {
            return nodeId_.compareTo(nodeId);
        } else if (lamport_.get() < lamport) {
            return -1;
        } else if (lamport_.get() > lamport) {
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
    private final ConcurrentLinkedQueue<NodeIdentity> sentenceUpdateQueue_;
    
    private String sentence_;
    ConcurrentSkipListMap<NodeIdentity, Vector<String>> sentenceHistory_;

    // Ricart Agrawala
    String raSemaphore_;
    private AtomicInteger lamport_;
    Set<NodeIdentity> okSet_;
    private final ConcurrentLinkedQueue<NodeIdentity> replyQueue_;
    NodeIdentity nodeId_;

    private boolean iAmMaster_;
    int networkSize_;

    // Used for testing only.
    private volatile int index = 1;

}
