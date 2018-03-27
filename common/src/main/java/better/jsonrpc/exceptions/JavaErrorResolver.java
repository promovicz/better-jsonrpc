package better.jsonrpc.exceptions;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Error resolver that reflects Java exceptions into JSON-RPC
 * <p/>
 * May be considered inappropriate in some environments, but indispensable in others.
 * <p/>
 */
public class JavaErrorResolver implements ErrorResolver {

	public static final JavaErrorResolver INSTANCE = new JavaErrorResolver();

	public JsonError resolveError(
		Throwable t, Method method, List<JsonNode> arguments) {
		return new JsonError(0, t.getMessage(), makeErrorData(t));
	}

	private JavaErrorData makeErrorData(Throwable t) {
		return new JavaErrorData(t.getClass().getName(), t.getMessage());
	}

}
