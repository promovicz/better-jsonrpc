package better.jsonrpc.exception;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unchecked Exception thrown by a JSON-RPC client when
 * an error occurs.
 */
@SuppressWarnings("serial")
public class JsonRpcException extends RuntimeException {

	private int code;
	private JsonNode data;

    public JsonRpcException(String message) {
        super(message);
        this.code = -1;
        this.data = null;
    }

	/**
	 * Creates an exception
	 * @param code the code from the server
	 * @param message the message from the server
	 * @param data the data from the server
	 */
	public JsonRpcException(int code, String message, JsonNode data) {
		super(message);
		this.code 	= code;
		this.data	= data;
	}

	/**
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @return the data
	 */
	public JsonNode getData() {
		return data;
	}

}
