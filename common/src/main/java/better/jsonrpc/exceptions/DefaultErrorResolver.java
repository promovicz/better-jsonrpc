package better.jsonrpc.exceptions;

import better.jsonrpc.exception.JsonRpcException;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Default error resolver
 * <p/>
 * Reflects JSON-RPC exceptions and nothing else.
 * <p/>
 * XXX Will pass exceptions from another node if calling recursively.
 * <p/>
 */
public class DefaultErrorResolver implements ErrorResolver {

    public static final DefaultErrorResolver INSTANCE = new DefaultErrorResolver();

    @Override
    public JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments) {
        if(t instanceof JsonRpcException) {
            JsonRpcException rpcException = (JsonRpcException)t;
            return new JsonError(
                    rpcException.getCode(),
                    rpcException.getMessage(),
                    rpcException.getData());
        } else {
            // hide anything else
            return new JsonError(0, "An error occured", null);
        }
    }

}
