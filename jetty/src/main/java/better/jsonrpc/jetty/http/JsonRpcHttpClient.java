package better.jsonrpc.jetty.http;

import better.jsonrpc.client.JsonRpcClientRequest;
import better.jsonrpc.core.JsonRpcTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ByteArrayBuffer;

import java.io.IOException;
import java.net.URI;

public class JsonRpcHttpClient extends JsonRpcTransport {

    private URI mUri;

    private String mContentType;

    private HttpClient mClient;

    public JsonRpcHttpClient(URI serviceUri, String contentType, HttpClient client, ObjectMapper mapper) {
        super(mapper);
        mUri = serviceUri;
        mContentType = contentType;
        mClient = client;
    }

    public HttpClient getHttpClient() {
        return mClient;
    }

    private void sendHttpRequest(JsonRpcClientRequest rpcRequest) throws IOException {
        // serialize the request
        ObjectNode requestNode = rpcRequest.getRequest();
        byte[] data = getMapper().writeValueAsBytes(requestNode);
        ByteArrayBuffer bytes = new ByteArrayBuffer(data);
        // build the HTTP exchange
        JsonRpcHttpExchange exchange = new JsonRpcHttpExchange(this, rpcRequest);
        exchange.setMethod("POST");
        exchange.setURI(mUri);
        exchange.setRequestContent(bytes);
        exchange.setRequestContentType(mContentType);
        // perform the exchange
        mClient.send(exchange);
    }

    @Override
    public void sendRequest(JsonRpcClientRequest request) throws IOException {
        sendHttpRequest(request);
    }

    @Override
    public void sendNotification(JsonRpcClientRequest notification) throws IOException {
        sendHttpRequest(notification);
    }

    @Override
    public void sendResponse(ObjectNode response) throws IOException {
        throw new RuntimeException("Can't send a JSON-RPC response on an HTTP transport");
    }

}
