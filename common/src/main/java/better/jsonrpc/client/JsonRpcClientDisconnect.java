package better.jsonrpc.client;

public class JsonRpcClientDisconnect extends JsonRpcClientException {

    public JsonRpcClientDisconnect() {
        super(-1, "JSON-RPC disconnect", null);
    }

}
