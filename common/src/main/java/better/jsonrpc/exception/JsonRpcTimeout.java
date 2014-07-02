package better.jsonrpc.exception;

import better.jsonrpc.exception.JsonRpcException;

public class JsonRpcTimeout extends JsonRpcException {

    public JsonRpcTimeout() {
        super("JSON-RPC request timed out");
    }

}
