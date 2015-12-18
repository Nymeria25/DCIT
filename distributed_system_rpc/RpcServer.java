package distributed_system_rpc;

import java.io.IOException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class RpcServer {
 
    RpcServer(int port) throws IOException, XmlRpcException {
      
        webServer_ = new WebServer(port);
        xmlRpcServer_ = webServer_.getXmlRpcServer();
        
        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        ConnectionUpdaterService connectionUpdater = new ConnectionUpdaterImpl();
        phm.setRequestProcessorFactoryFactory(new ConnectionUpdaterFactory(connectionUpdater));
        phm.addHandler(ConnectionUpdaterService.class.getName(), ConnectionUpdaterImpl.class);
        xmlRpcServer_.setHandlerMapping(phm);

        //default configuration for the xmlRpcServer
        XmlRpcServerConfigImpl serverConfig
                = (XmlRpcServerConfigImpl) xmlRpcServer_.getConfig();
        serverConfig.setEnabledForExtensions(true);
        serverConfig.setContentLengthOptional(false);        
    }
    
    void startServer() throws IOException{
        webServer_.start();
    }
    
    private XmlRpcServer xmlRpcServer_;
    private WebServer webServer_;
}
