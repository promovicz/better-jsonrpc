package better.jsonrpc.jetty.http;

import better.jsonrpc.client.JsonRpcClientRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.Buffer;

import java.io.IOException;

/**
 * Represents a single JSON-RPC HTTP exchange
 */
public class JsonRpcHttpExchange extends ContentExchange {

    JsonRpcHttpClient mClient;

    JsonRpcClientRequest mRequest;

    public JsonRpcHttpExchange(JsonRpcHttpClient client, JsonRpcClientRequest request) {
        mClient = client;
        mRequest = request;
    }

    @Override
    protected void onResponseComplete() throws IOException {
        super.onResponseComplete();
        if(getResponseStatus() == 200) {
            byte[] bytes = getResponseContentBytes();
            ObjectNode response = mClient.getMapper().readValue(bytes, ObjectNode.class);
            mClient.getClient().handleResponse(response, mClient);
        } else {
            mRequest.handleException(new Exception("Duh!? " + getResponseStatus()));
        }
    }

    @Override
    protected void onConnectionFailed(Throwable x) {
        super.onConnectionFailed(x);
        mRequest.handleDisconnect();
    }

    @Override
    protected void onException(Throwable x) {
        super.onException(x);
        mRequest.handleException(x);
    }

    @Override
    protected void onExpire() {
        super.onExpire();
        mRequest.handleException(new Exception("HTTP timeout"));
    }

}
