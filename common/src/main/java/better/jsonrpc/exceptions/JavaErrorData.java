package better.jsonrpc.exceptions;

public class JavaErrorData {

    private String exceptionTypeName;
    private String message;

    /**
     * Creates it.
     * @param exceptionTypeName the exception type name
     * @param message the message
     */
    public JavaErrorData(String exceptionTypeName, String message) {
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
