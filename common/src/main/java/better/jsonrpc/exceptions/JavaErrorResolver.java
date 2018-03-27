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

	/**
	 * {@inheritDoc}
	 */
	public JsonError resolveError(
		Throwable t, Method method, List<JsonNode> arguments) {
		return new JsonError(0, t.getMessage(),
			new ErrorData(t.getClass().getName(), t.getMessage()));
	}

	/**
	 * Data that is added to an error.
	 *
	 */
	public static class ErrorData {

		private String exceptionTypeName;
		private String message;

		/**
		 * Creates it.
		 * @param exceptionTypeName the exception type name
		 * @param message the message
		 */
		public ErrorData(String exceptionTypeName, String message) {
			this.exceptionTypeName 	= exceptionTypeName;
			this.message			= message;
		}

		/**
		 * @return the exceptionTypeName
		 */
		public String getExceptionTypeName() {
			return exceptionTypeName;
		}

		/**
		 * @return the message
		 */
		public String getMessage() {
			return message;
		}

	}

}
