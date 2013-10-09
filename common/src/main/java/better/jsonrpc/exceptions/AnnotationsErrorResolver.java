package better.jsonrpc.exceptions;

import better.jsonrpc.annotations.JsonRpcError;
import better.jsonrpc.annotations.JsonRpcErrors;
import better.jsonrpc.exceptions.DefaultErrorResolver.ErrorData;
import better.jsonrpc.util.ReflectionUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * {@link ErrorResolver} that uses annotations.
 */
public class AnnotationsErrorResolver
	implements ErrorResolver {

	public static final AnnotationsErrorResolver INSTANCE = new AnnotationsErrorResolver();

	/**
	 * {@inheritDoc}
	 */
	public JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments) {

		// use annotations to map errors
		JsonRpcErrors errors = ReflectionUtil.getAnnotation(method, JsonRpcErrors.class);
		if (errors!=null) {
			for (JsonRpcError em : errors.value()) {
				if (em.exception().isInstance(t)) {
					String message = em.message()!=null && em.message().trim().length() > 0
						? em.message()
						: t.getMessage();
					return new JsonError(em.code(), message,
						new ErrorData(em.exception().getName(), message));
				}
			}
		}

		//  none found
		return null;
	}

}
