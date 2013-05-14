package better.jsonrpc.client;

public class JsonRpcClientTimeout extends JsonRpcClientException {

    public JsonRpcClientTimeout() {
        super(-1, "JSON-RPC timeout", null);
    }

}
