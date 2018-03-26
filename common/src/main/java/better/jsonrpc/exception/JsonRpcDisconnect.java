package better.jsonrpc.exception;

public class JsonRpcDisconnect extends JsonRpcException {

    public JsonRpcDisconnect() {
        super("JSON-RPC request aborted due to disconnect");
    }

}
