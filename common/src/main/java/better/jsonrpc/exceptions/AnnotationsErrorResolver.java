package better.jsonrpc.exceptions;

import better.jsonrpc.annotations.JsonRpcTranslateException;
import better.jsonrpc.annotations.JsonRpcTranslateExceptions;
import better.jsonrpc.util.ReflectionUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * {@link ErrorResolver} that uses annotations.
 */
public class AnnotationsErrorResolver implements ErrorResolver {

	public static final AnnotationsErrorResolver INSTANCE = new AnnotationsErrorResolver();

	/**
	 * {@inheritDoc}
	 */
	public JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments) {
		Class<?> clazz = method.getDeclaringClass();
		// plural first
		JsonRpcTranslateExceptions plural = ReflectionUtil.getAnnotation(method, JsonRpcTranslateExceptions.class);
		if (plural != null) {
			for (JsonRpcTranslateException err : plural.value()) {
				if (err.exception().isInstance(t)) {
					return translateError(err, t);
				}
			}
		}
		// now singular
		JsonRpcTranslateException singular = ReflectionUtil.getAnnotation(method, JsonRpcTranslateException.class);
		if (singular != null) {
			if (singular.exception().isInstance(t)) {
				return translateError(singular, t);
			}
		}
		// also class plural
		JsonRpcTranslateExceptions classPlural = ReflectionUtil.getAnnotation(clazz, JsonRpcTranslateExceptions.class);
		if (classPlural != null) {
			for (JsonRpcTranslateException err : classPlural.value()) {
				if (err.exception().isInstance(t)) {
					return translateError(err, t);
				}
			}
		}
		// and class singular
		JsonRpcTranslateException classSingular = ReflectionUtil.getAnnotation(clazz, JsonRpcTranslateException.class);
		if (classSingular != null) {
			if (classSingular.exception().isInstance(t)) {
				return translateError(classSingular, t);
			}
		}
		// found nothing
		return null;
	}

	private JsonError translateError(JsonRpcTranslateException declaration, Throwable t) {
		String message = declaration.message().trim().length() > 0
						? declaration.message()
						: t.getMessage();
		return new JsonError(declaration.code(), message);
	}

}
