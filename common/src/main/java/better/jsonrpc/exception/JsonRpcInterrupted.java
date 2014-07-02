package better.jsonrpc.exception;

public class JsonRpcInterrupted extends JsonRpcException {

    public JsonRpcInterrupted() {
        super("JSON-RPC request interrupted locally");
    }

}
