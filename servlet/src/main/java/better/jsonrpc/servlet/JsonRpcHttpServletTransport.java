package better.jsonrpc.servlet;

import better.jsonrpc.client.JsonRpcClientRequest;
import better.jsonrpc.core.JsonRpcTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class JsonRpcHttpServletTransport extends JsonRpcTransport {

    private HttpServletRequest mRequest;
    private HttpServletResponse mResponse;

    JsonRpcHttpServletTransport(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response) {
        super(mapper);
        mRequest = request;
        mResponse = response;
    }

    @Override
    public void sendResponse(ObjectNode response) throws IOException {
        ObjectMapper mapper = getMapper();
        OutputStream responseStream = mResponse.getOutputStream();
        mapper.writeValue(responseStream, response);
        mResponse.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public void sendRequest(JsonRpcClientRequest request) throws IOException {
        mResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        throw new RuntimeException("Cannot send a request to answer an HTTP JSON-RPC request");
    }

    @Override
    public void sendNotification(JsonRpcClientRequest notification) throws IOException {
        mResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        throw new RuntimeException("Cannot send a notification to answer an HTTP JSON-RPC request");
    }

}
