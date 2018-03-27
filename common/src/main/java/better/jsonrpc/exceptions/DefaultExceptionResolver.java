package better.jsonrpc.exceptions;

import better.jsonrpc.exception.JsonRpcException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Default exception resolver
 * <p/>
 * Always throws a JsonRpcException instance.
 * <p/>
 */
public class DefaultExceptionResolver implements ExceptionResolver {

    public static final DefaultExceptionResolver INSTANCE = new DefaultExceptionResolver();

    @Override
    public Throwable resolveException(ObjectNode response) {
        // get the error object
        ObjectNode errorObject = ObjectNode.class.cast(response.get("error"));
        // create a JSON-RPC exception
        return new JsonRpcException(
                errorObject.get("code").asInt(),
                errorObject.get("message").asText(),
                errorObject.get("data"));
    }

}
