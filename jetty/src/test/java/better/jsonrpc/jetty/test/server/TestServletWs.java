package better.jsonrpc.jetty.test.server;

import better.jsonrpc.jetty.servlet.JsonRpcWsServlet;
import better.jsonrpc.jetty.test.rpc.ISimpleServer;
import better.jsonrpc.jetty.test.rpc.SimpleServer;
import better.jsonrpc.server.JsonRpcServer;

import javax.servlet.http.HttpServletRequest;

public class TestServletWs extends JsonRpcWsServlet {

    JsonRpcServer mServer;
    SimpleServer mHandler;

    public TestServletWs() {
        mServer = new JsonRpcServer(ISimpleServer.class);
        mHandler = new SimpleServer();
    }

    @Override
    protected JsonRpcServer getServer(HttpServletRequest request, String protocol) {
        return mServer;
    }

    @Override
    protected Object getHandler(HttpServletRequest request, String protocol) {
        return mHandler;
    }

}
