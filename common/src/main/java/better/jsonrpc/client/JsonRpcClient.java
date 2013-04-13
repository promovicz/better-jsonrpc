package better.jsonrpc.client;

import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.exceptions.DefaultExceptionResolver;
import better.jsonrpc.exceptions.ExceptionResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A JSON-RPC client.
 */
public class JsonRpcClient {

    /** Default request timeout (msecs) */
    public static final long DEFAULT_REQUEST_TIMEOUT = 15 * 1000;

    /** Global logger for clients */
	private static final Logger LOG = Logger.getLogger(JsonRpcClient.class.getName());

    /** JSON-RPC version to pretend speaking */
	private static final String JSON_RPC_VERSION = "2.0";


    /** Request timeout for this client (msecs) */
    private long mRequestTimeout = DEFAULT_REQUEST_TIMEOUT;

    /** JSON object mapper being used */
	private ObjectMapper mMapper;

    /** Generator for request IDs */
	private Random mIdGenerator;

    /** Exception converter */
	private ExceptionResolver mExceptionResolver = DefaultExceptionResolver.INSTANCE;

    /** Table of outstanding requests */
    private Hashtable<String, JsonRpcClientRequest> mOutstandingRequests =
            new Hashtable<String, JsonRpcClientRequest>();

    /** Listener for connection state changes */
    private JsonRpcConnection.Listener mConnectionListener =
            new JsonRpcConnection.Listener() {
                @Override
                public void onOpen(JsonRpcConnection connection) {
                    handleConnectionChange(connection);
                }
                @Override
                public void onClose(JsonRpcConnection connection) {
                    handleConnectionChange(connection);
                }
            };


    /**
     * Creates a client that uses the default {@link ObjectMapper}
     * to map to and from JSON and Java objects.
     */
    public JsonRpcClient() {
        this(new ObjectMapper());
    }

	/**
	 * Creates a client that uses the given {@link ObjectMapper} to
	 * map to and from JSON and Java objects.
	 * @param mapper the {@link ObjectMapper}
	 */
	public JsonRpcClient(ObjectMapper mapper) {
		this.mMapper = mapper;
		this.mIdGenerator = new Random(System.currentTimeMillis());
	}


    /**
     * Returns the {@link ObjectMapper} that the client
     * is using for JSON marshalling.
     * @return the {@link ObjectMapper}
     */
    public ObjectMapper getObjectMapper() {
        return mMapper;
    }

    /**
     * Returns the {@link ExceptionResolver} for this client
     */
    public ExceptionResolver getExceptionResolver() {
        return mExceptionResolver;
    }

    /**
     * Set the {@link ExceptionResolver} for this client
     * @param mExceptionResolver the exceptionResolver to set
     */
    public void setExceptionResolver(ExceptionResolver mExceptionResolver) {
        this.mExceptionResolver = mExceptionResolver;
    }

    /**
     * Get request timeout (in msecs)
     */
    public long getRequestTimeout() {
        return mRequestTimeout;
    }

    /**
     * Set request timeout (in msecs)
     * @param mRequestTimeout
     */
    public void setRequestTimeout(long mRequestTimeout) {
        this.mRequestTimeout = mRequestTimeout;
    }

    
    /**
     * Generate a new request id and return it
     * @return
     */
    private String generateId() {
        return mIdGenerator.nextLong()+"";
    }


    /**
     * Handle binding to a connection
     */
    public void bindConnection(JsonRpcConnection connection) {
        connection.addListener(mConnectionListener);
    }

    /**
     * Handle unbinding from a connection
     */
    public void unbindConnection(JsonRpcConnection connection) {
        connection.removeListener(mConnectionListener);
    }

    /**
     * Send a request through the connection
     */
    public void sendRequest(JsonRpcConnection connection, ObjectNode request) throws Exception {
        // log request
        LOG.info("sending request " + request.toString());
        // send it
        connection.sendRequest(request);
    }

    /**
     * Send a notification through the connection
     */
    public void sendNotification(JsonRpcConnection connection, ObjectNode notification) throws Exception {
        // log notification
        LOG.info("sending notification " + notification.toString());
        // send it
        connection.sendNotification(notification);
    }

    /**
     * Handle a connection state change (connect/disconnect)
     *
     * This will remove all requests associated with the connection indicated.
     *
     * @param connection
     */
    private void handleConnectionChange(JsonRpcConnection connection) {
        synchronized (mOutstandingRequests) {
            // vector to collect matched requests into
            Vector<JsonRpcClientRequest> matches = new Vector<JsonRpcClientRequest>();
            // for every outstanding request
            Enumeration<JsonRpcClientRequest> reqs = mOutstandingRequests.elements();
            while(reqs.hasMoreElements()) {
                JsonRpcClientRequest req = reqs.nextElement();
                // if the request belongs to the changed connection
                if(req.getConnection() == connection) {
                    // unblock the requestor
                    req.handleDisconnect();
                    // and remember the request
                    matches.add(req);
                }
            }
            // for all relevant requests
            for(JsonRpcClientRequest req: matches) {
                // remove request from table
                mOutstandingRequests.remove(req.getId());
            }
        }
    }


    /**
     * Invoke the method specified via the given connection
     *
     * Requests submitted via this method may block and will
     * be tracked by the client in its outstanding request table.
     *
     * @param methodName
     * @param arguments
     * @param returnType
     * @param connection
     * @return remote return value
     * @throws Throwable
     */
	public Object invokeMethod(String methodName, Object arguments, Type returnType, JsonRpcConnection connection)
		throws Throwable {
        Object result = null;
        // generate request id
        String id = generateId();
        // construct the JSON request node
        ObjectNode requestNode = createRequest(methodName, arguments, id);
        // construct the request state object
        JsonRpcClientRequest request = new JsonRpcClientRequest(id, requestNode, connection);
        // add the request to client state
        synchronized (mOutstandingRequests) {
            mOutstandingRequests.put(id, request);
        }
        // request execution
        try {
            // send request
            sendRequest(connection, requestNode);
            // wait for response or other result
            result = request.waitForResponse(returnType);
        } catch (Throwable t) {
            // abort the request
            request.handleException(t);
            // rethrow for library user to handle
            throw t;
        } finally {
            // remove request from client state
            synchronized (mOutstandingRequests) {
                mOutstandingRequests.remove(id);
            }
        }
        // return final result
        return result;
	}

    /**
     * Invoke the method specified via the given connection
     *
     * Note that notifications are never tracked by the client.
     *
     * @param methodName
     * @param arguments
     * @param connection
     */
	public void invokeNotification(String methodName, Object arguments, JsonRpcConnection connection)
        throws Throwable {
        // create the JSON request object
		ObjectNode requestNode = createRequest(methodName, arguments, null);
        // create client request object
        JsonRpcClientRequest request = new JsonRpcClientRequest(null, requestNode, connection);
        // execute the request
        try {
            // send request
		    sendNotification(connection, requestNode);
        } catch (Throwable t) {
            // abort the request
            request.handleException(t);
            //
            throw t;
        }
	}

    /**
     * Handle an incoming JSON response
     * @param response to process
     * @param connection on which the response arrived
     * @return true if the response was accepted
     */
	public boolean handleResponse(ObjectNode response, JsonRpcConnection connection) {
		JsonNode idNode = response.get("id");
        // check the id, we only use strings
		if(idNode != null && idNode.isTextual()) {
			String id = idNode.asText();
            // log response
            LOG.info("received response " + response.toString());
            // retrieve the request from the client table
            JsonRpcClientRequest req = null;
            synchronized (mOutstandingRequests) {
                req = mOutstandingRequests.get(id);
            }
            // if there was an actual request
			if(req != null && req.getConnection() == connection) {
                // handle the response, unblocking the requestor
				req.handleResponse(response);
                // we have handled the request
                return true;
			}
		}
        // we have not handled the request
        return false;
	}

    /**
     * Creates a JSON request node.
     * @param methodName the method name
     * @param arguments the arguments
     * @param id the optional id
     * @return the new request
     */
    private ObjectNode createRequest(
            String methodName, Object arguments, String id) {

        // create the request
        ObjectNode request = mMapper.createObjectNode();

        // add id
        if (id!=null) { request.put("id", id); }

        // add protocol and method
        request.put("jsonrpc", JSON_RPC_VERSION);
        request.put("method", methodName);

        // default empty arguments, will be replaced further down
        request.put("params", mMapper.valueToTree(new Object[0]));

        // object array args
        if (arguments!=null && arguments.getClass().isArray()) {
            Object[] args = Object[].class.cast(arguments);
            if (args.length>0) {
                request.put("params", mMapper.valueToTree(Object[].class.cast(arguments)));
            }

            // collection args
        } else if (arguments!=null && Collection.class.isInstance(arguments)) {
            if (!Collection.class.cast(arguments).isEmpty()) {
                request.put("params", mMapper.valueToTree(arguments));
            }

            // map args
        } else if (arguments!=null && Map.class.isInstance(arguments)) {
            if (!Map.class.cast(arguments).isEmpty()) {
                request.put("params", mMapper.valueToTree(arguments));
            }

            // other args
        } else if (arguments!=null) {
            request.put("params", mMapper.valueToTree(arguments));
        }

        // log
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "JSON-PRC Request: "+request.toString());
        }

        // return the request
        return request;
    }

}
