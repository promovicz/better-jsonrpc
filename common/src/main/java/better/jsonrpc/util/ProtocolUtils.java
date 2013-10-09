package better.jsonrpc.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;

public class ProtocolUtils {
	
	/**
	 * Parses an ID.
	 * @param node
	 * @return
	 */
	public static Object parseId(JsonNode node) {
		if (node==null || node.isNull()) {
			return null;
		} else if (node.isDouble()) {
			return node.asDouble();
		} else if (node.isFloatingPointNumber()) {
			return node.asDouble();
		} else if (node.isInt()) {
			return node.asInt();
		} else if (node.isIntegralNumber()) {
			return node.asInt();
		} else if (node.isLong()) {
			return node.asLong();
		} else if (node.isTextual()) {
			return node.asText();
		}
		throw new IllegalArgumentException("Unknown id type");
	}
	
	/**
	 * Convenience method for creating an error response.
	 *
	 * @param jsonRpc the jsonrpc string
	 * @param id the id
	 * @param code the error code
	 * @param message the error message
	 * @param data the error data (if any)
	 * @return the error response
	 */
	public static ObjectNode createErrorResponse(
			ObjectMapper mapper,
			String jsonRpc, Object id, int code, String message, Object data) {
		ObjectNode response = mapper.createObjectNode();
		ObjectNode error = mapper.createObjectNode();
		error.put("code", code);
		error.put("message", message);
		if (data!=null) {
			error.put("data",  mapper.valueToTree(data));
		}
		response.put("jsonrpc", jsonRpc);
		if (Integer.class.isInstance(id)) {
			response.put("id", Integer.class.cast(id).intValue());
		} else if (Long.class.isInstance(id)) {
			response.put("id", Long.class.cast(id).longValue());
		} else if (Float.class.isInstance(id)) {
			response.put("id", Float.class.cast(id).floatValue());
		} else if (Double.class.isInstance(id)) {
			response.put("id", Double.class.cast(id).doubleValue());
		} else if (BigDecimal.class.isInstance(id)) {
			response.put("id", BigDecimal.class.cast(id));
		} else {
			response.put("id", String.class.cast(id));
		}
		response.put("error", error);
		return response;
	}

	/**
	 * Creates a sucess response.
	 * @param jsonRpc
	 * @param id
	 * @param result
	 * @return
	 */
	public static ObjectNode createSuccessResponse(
			ObjectMapper mapper,
			String jsonRpc, Object id, JsonNode result) {
		ObjectNode response = mapper.createObjectNode();
		response.put("jsonrpc", jsonRpc);
		if (Integer.class.isInstance(id)) {
			response.put("id", Integer.class.cast(id).intValue());
		} else if (Long.class.isInstance(id)) {
			response.put("id", Long.class.cast(id).longValue());
		} else if (Float.class.isInstance(id)) {
			response.put("id", Float.class.cast(id).floatValue());
		} else if (Double.class.isInstance(id)) {
			response.put("id", Double.class.cast(id).doubleValue());
		} else if (BigDecimal.class.isInstance(id)) {
			response.put("id", BigDecimal.class.cast(id));
		} else {
			response.put("id", String.class.cast(id));
		}
		response.put("result", result);
		return response;
	}
}
