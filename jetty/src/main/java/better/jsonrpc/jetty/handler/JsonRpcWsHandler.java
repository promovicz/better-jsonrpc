package better.jsonrpc.jetty.handler;

import better.jsonrpc.jetty.websocket.JsonRpcWsTransport;
import better.jsonrpc.server.JsonRpcServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;

public class JsonRpcWsHandler extends WebSocketHandler {

    JsonRpcServer mServer;
    Object        mHandler;
    ObjectMapper  mMapper;

    public JsonRpcWsHandler(JsonRpcServer server, Object handler, ObjectMapper mapper) {
        mServer = server;
        mHandler = handler;
        mMapper = mapper;
    }

    public JsonRpcWsHandler(JsonRpcServer server, Object handler) {
        this(server, handler, new ObjectMapper());
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        ObjectMapper mapper = getMapper(request, protocol);
        JsonRpcWsTransport transport = new JsonRpcWsTransport(mapper, true);
        JsonRpcServer server = getServer(request, protocol);
        Object handler = getHandler(request, protocol);
        transport.bindServer(server, handler);
        return transport;
    }

    protected ObjectMapper getMapper(HttpServletRequest request, String protocol) {
        return mMapper;
    }

    protected JsonRpcServer getServer(HttpServletRequest request, String protocol) {
        return mServer;
    }

    protected Object getHandler(HttpServletRequest request, String protocol) {
        return mHandler;
    }

}
