
package distributed_system_rpc;

/**
 * ConnectionUpdaterFactory is used to implement a stateful server.
 * A factory object is provided to the PropertyHandlerMapping object, on server
 * side.
 */

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;

 public class ConnectionUpdaterFactory implements
      RequestProcessorFactoryFactory {

    public ConnectionUpdaterFactory(ConnectionUpdaterService cu) {
      connectionUpdater_ = cu;
      factory_ = new ConnectionUpdaterRequestProcessorFactory();
    }

    @Override
    public RequestProcessorFactory getRequestProcessorFactory(Class aClass)
         throws XmlRpcException {
      return factory_;
    }

    private class ConnectionUpdaterRequestProcessorFactory implements 
            RequestProcessorFactory {
      @Override
      public Object getRequestProcessor(XmlRpcRequest xmlRpcRequest)
          throws XmlRpcException {
        return connectionUpdater_;
      }
    }
    
    private final RequestProcessorFactory factory_;
    private final ConnectionUpdaterService connectionUpdater_;
  }