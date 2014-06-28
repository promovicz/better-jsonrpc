package better.jsonrpc.jetty.test.server;

import better.jsonrpc.jetty.test.rpc.ISimpleServer;
import better.jsonrpc.jetty.test.rpc.SimpleServer;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.servlet.JsonRpcHttpServlet;

import javax.servlet.http.HttpServletRequest;

public class TestServletHttp extends JsonRpcHttpServlet {

    JsonRpcServer mServer;
    SimpleServer mHandler;

    public TestServletHttp() {
        mServer = new JsonRpcServer(ISimpleServer.class);
        mHandler = new SimpleServer();
    }

    @Override
    protected JsonRpcServer getServer(HttpServletRequest request) {
        return mServer;
    }

    @Override
    protected Object getHandler(HttpServletRequest request) {
        return mHandler;
    }

}
