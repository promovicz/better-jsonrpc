package better.jsonrpc.exception;

import better.jsonrpc.exception.JsonRpcException;

public class JsonRpcDisconnect extends JsonRpcException {

    public JsonRpcDisconnect() {
        super("JSON-RPC request aborted due to disconnect");
    }

}
