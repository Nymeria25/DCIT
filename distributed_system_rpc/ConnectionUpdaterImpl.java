package distributed_system_rpc;

import java.util.HashMap;

public class ConnectionUpdaterImpl implements ConnectionUpdaterService {

    private HashMap<String, Integer> connectedNodes;
    private volatile int index = 1;

    @Override
    public int echo(String IPAddress, int port, String msg) {
      System.out.println(index+": "+msg);
      index++;
      return index;
    }
    
    @Override
    public String join(String IPAddress, int port) {
        if (!connectedNodes.containsKey(IPAddress)) {
            connectedNodes.put(IPAddress, port);
            return "Join accepted.";
        } else {
            return "Join refused.";
        }
    }
}
