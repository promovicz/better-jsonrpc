package better.jsonrpc.jetty.test.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class TestServer {

    private Server mServer;
    private SelectChannelConnector mConnector;
    private ServletHandler mServletHandler;

    private ObjectMapper  mJsonMapper;

    public TestServer() {
        mJsonMapper = new ObjectMapper();

        mServletHandler = new ServletHandler();
        mServletHandler.addServletWithMapping(TestServletWs.class, "/ws");
        mServletHandler.addServletWithMapping(TestServletHttp.class, "/http");

        mConnector = new SelectChannelConnector();
        mConnector.setThreadPool(new QueuedThreadPool(10));
        mConnector.setHost("localhost");
        mConnector.setPort(0);

        mServer = new Server();
        mServer.setThreadPool(new QueuedThreadPool(10));
        mServer.setHandler(mServletHandler);
        mServer.setConnectors(new Connector[]{mConnector});
    }

    public int getLocalPort() {
        return mConnector.getLocalPort();
    }

    public void start() throws Exception {
        mServer.start();
    }

    public void stop() throws Exception {
        mServer.stop();
    }

    public void join() throws InterruptedException {
        mServer.join();
    }

}
