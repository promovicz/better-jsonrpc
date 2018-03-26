package better.jsonrpc.exception;

public class JsonRpcTimeout extends JsonRpcException {

    public JsonRpcTimeout() {
        super("JSON-RPC request timed out");
    }

}
