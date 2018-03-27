package better.jsonrpc.exceptions;

/**
 * A JSON error.
 */
public class JsonError {

    private int code;
    private String message;
    private Object data;

    public JsonError(int code, String message) {
        this(code, message, null);
    }

    /**
     * Creates the error.
     * @param code the code
     * @param message the message
     * @param data the data
     */
    public JsonError(int code, String message, Object data) {
        this.code 		= code;
        this.message	= message;
        this.data		= data;
    }

    /**
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the data
     */
    public Object getData() {
        return data;
    }

}
