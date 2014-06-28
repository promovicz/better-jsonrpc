package better.jsonrpc.jetty.servlet;

import better.jsonrpc.jetty.websocket.JsonRpcWsTransport;
import better.jsonrpc.server.JsonRpcServer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.undercouch.bson4jackson.BsonFactory;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public abstract class JsonRpcWsServlet extends WebSocketServlet {

    private static final String PROTOCOL_JSON = "jsonrpc/json";
    private static final String PROTOCOL_BSON = "jsonrpc/bson";

    ObjectMapper mJsonMapper;
    ObjectMapper mBsonMapper;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mJsonMapper = new ObjectMapper(new JsonFactory());
        mBsonMapper = new ObjectMapper(new BsonFactory());
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        // determine if we need to send in binary
        boolean binary = false;
        if(protocol.equals(PROTOCOL_BSON)) {
            binary = true;
        }
        // get the object mapper
        ObjectMapper mapper = getMapper(request, protocol);
        if(mapper == null) {
            throw new RuntimeException("Unknown protocol " + protocol);
        }
        // create transport
        JsonRpcWsTransport transport = new JsonRpcWsTransport(mapper, true);
        transport.setAcceptTextMessages(!binary);
        transport.setSendBinaryMessages(binary);
        // get server and handler
        JsonRpcServer server = getServer(request, protocol);
        Object handler = getHandler(request, protocol);
        // bind the transport and return it
        transport.bindServer(server, handler);
        return transport;
    }

    protected ObjectMapper getMapper(HttpServletRequest request, String protocol) {
        ObjectMapper mapper = null;
        if(protocol.equals(PROTOCOL_JSON)) {
            mapper = mJsonMapper;
        } else if(protocol.equals(PROTOCOL_BSON)) {
            mapper = mBsonMapper;
        }
        return mapper;
    }

    protected abstract JsonRpcServer getServer(HttpServletRequest request, String protocol);
    protected abstract Object        getHandler(HttpServletRequest request, String protocol);

}
