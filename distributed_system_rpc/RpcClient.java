package distributed_system_rpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

public class RpcClient {

    RpcClient(NodeIdentity nodeServerNodeId, NodeIdentity primaryServerNodeId)
            throws MalformedURLException {
        SetRPCWaitingTimes();
        
        appendedWords_ = new ArrayList<>();

        nodeServerNodeId_ = nodeServerNodeId;
        ClientFactory nodeServerFactory = CreateClientFactory(
                nodeServerNodeId_);
        nodeServer_ = (ConnectionUpdaterService) nodeServerFactory.
                newInstance(ConnectionUpdaterService.class);
        
        JoinNetworkImpl(nodeServer_, primaryServerNodeId);
    }
    
    public void masterJob() throws InterruptedException {
        if (nodeServer_.isMaster()) {
            Thread.sleep(23000);
            System.out.println("This is master!");
            System.exit(0);
        }
    }

    public void ReadWrite() throws InterruptedException {
        try {
            String algorithm = nodeServer_.getReadWriteStatus();
            if (algorithm.length() > 0) {
                String appendedWords = ClientReadWrite(algorithm);

                System.out.println("Master's sentence: ");
                String masterString = nodeServer_.getSentenceFromMaster();
                System.out.println(masterString);
                
                int numNonAppended = 0;
                for (String word : appendedWords_) {
                    if (!masterString.contains(word)) {
                        numNonAppended++;
                    }
                }
                
                System.out.println("Number of words not appended: " + numNonAppended);
                System.exit(0);
            }
        } catch (Exception e) {
            System.err.println("The node server is dead.");
        }

    }

    private String ClientReadWrite(String algorithm) throws InterruptedException {
        Dictionary dictionary = new Dictionary();

        int totalTime = 20000; // ms
        long startTime = System.currentTimeMillis();
        String clientSentence = "";

        while (System.currentTimeMillis() - startTime < totalTime) {
            Thread.sleep(generateRandomWaitingTime(500, 1000));
            try {
                clientSentence = nodeServer_.getSentenceFromMaster();
                
                String word = dictionary.getRandomWord();
                System.out.println(word);
                appendedWords_.add(word);
                clientSentence += word;
                
                if (algorithm.equals("Centralized Mutual Exclusion")) {
                    nodeServer_.performSentenceUpdate(nodeServerNodeId_.toString());
                    nodeServer_.writeSentenceToMaster(clientSentence);
                    nodeServer_.doneSentenceUpdate(nodeServerNodeId_.toString());
                } else {
                    nodeServer_.ricartAgrawalaReq(nodeServerNodeId_.toString());
                    nodeServer_.writeSentenceToMaster(clientSentence);
                    nodeServer_.doneRicartAgrawalaReq();
                }
                
            } catch (Exception e) {

            }
        }
        return clientSentence;
    }

 
    // Runs the client console and reads/performs manual commands from keyboard.
    public void RunClientConsole() throws XmlRpcException,
            MalformedURLException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Command: ");
            String command = scanner.next();

            if ("$echo".equals(command)) {
                String message = scanner.next();
               // this.ClientEcho(message);
            } else if ("$rwc".equals(command)) {
               nodeServer_.readWriteReady("Centralized Mutual Exclusion");
            } else if ("$rwra".equals(command)) {
               nodeServer_.readWriteReady("Centralized Mutual Exclusion");
            } else if ("$print".equals(command)) {
                nodeServer_.print();
            } else if ("$help".equals(command)) {
                System.out.println("Possible commands:");
                System.out.println("$rwc read/write with Centralized Mutual Exclusion.");
                System.out.println("$rwra read/write with Ricart Agrawala.");
                System.out.println("$print prints the latest updated list of nodes in the network.");
                System.out.println("$kill signs off node.");
            } else if ("$kill".equals(command)) {
                SignOffImpl(nodeServer_, nodeServerNodeId_);
                System.exit(0);
            }
        }
    }

    // ------------------ Private  -------------------    
    private void SetRPCWaitingTimes() {
        xmlrpcConnTimeout_ = 10000;
        xmlrpcReplyTimeOut_ = 30000;
    }

    private void JoinNetworkImpl(ConnectionUpdaterService cu, NodeIdentity ni) {
        cu.performNetworkUpdate(nodeServerNodeId_.toString());
        cu.joinNetwork(ni.toString());
        cu.doneNetworkUpdate(nodeServerNodeId_.toString());
    }

    private void SignOffImpl(ConnectionUpdaterService cu, NodeIdentity ni) {
        cu.performNetworkUpdate(nodeServerNodeId_.toString());
        cu.signOff(ni.toString());
        cu.doneNetworkUpdate(nodeServerNodeId_.toString());
    }
    
    private ClientFactory CreateClientFactory(NodeIdentity nodeId) throws
            MalformedURLException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setEnabledForExtensions(true);
        config.setServerURL(new URL("http", nodeId.getKey(), nodeId.getValue(),
                "xmlrpc"));
        config.setConnectionTimeout(xmlrpcConnTimeout_);
        config.setReplyTimeout(xmlrpcReplyTimeOut_);

        // Creates an XmlRpcClient and instantiates a ClientFactory.
        XmlRpcClient rpcClient = new XmlRpcClient();
        rpcClient.setConfig(config);
        return new ClientFactory(rpcClient);
    }

    private static int generateRandomWaitingTime(int Min, int Max) {
        return Min + (int) (Math.random() * ((Max - Min) + 1));
    }

    
    // The connection updater service that works as the server function of this
    // node.
    ConnectionUpdaterService nodeServer_;
    // The first server of the network to which this node was connected (when
    // it was instantiated.
 
    private NodeIdentity nodeServerNodeId_;
    // ms for connection timeout and reply timeout.
    private int xmlrpcConnTimeout_, xmlrpcReplyTimeOut_;

    List<String> appendedWords_;
}
