package better.jsonrpc.exceptions;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Resolves {@link Throwable}s thrown by JSON-RPC services.
 */
public interface ErrorResolver {

	/**
	 * Resolves the error using the {@link Throwable} that
	 * was thrown by the given {@link Method} when passed
	 * the given {@code arguments}.  If the error can not
	 * be resolved then null is returned.
	 *
	 * @param t the {@link Throwable}
	 * @param method the {@link Method} that threw the {@link Throwable}
	 * @param arguments the {@code arguments} that were passed to the {@link Method}
	 * @return the {@link JsonError} or null if it couldn't be resolved
	 */
	JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments);

}
