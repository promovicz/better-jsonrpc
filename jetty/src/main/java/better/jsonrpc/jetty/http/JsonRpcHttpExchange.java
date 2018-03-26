package better.jsonrpc.jetty.http;

import better.jsonrpc.client.JsonRpcClientRequest;
import better.jsonrpc.exception.JsonRpcProtocolError;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.client.ContentExchange;

import java.io.IOException;

/**
 * Represents a single JSON-RPC HTTP exchange
 */
public class JsonRpcHttpExchange extends ContentExchange {

    private JsonRpcHttpClient mClient;

    private JsonRpcClientRequest mRequest;

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
            mRequest.handleLocalException(new JsonRpcProtocolError("Bad HTTP status code " + getResponseStatus()));
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
        mRequest.handleLocalException(x);
    }

    @Override
    protected void onExpire() {
        super.onExpire();
        mRequest.handleTimeout();
    }

}
