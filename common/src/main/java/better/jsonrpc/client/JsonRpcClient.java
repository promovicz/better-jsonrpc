package better.jsonrpc.client;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.exceptions.DefaultExceptionResolver;
import better.jsonrpc.exceptions.ExceptionResolver;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * A JSON-RPC client.
 */
public class JsonRpcClient {

    public static final long DEFAULT_REQUEST_TIMEOUT = 15 * 1000; // msec

	private static final Logger LOG = Logger.getLogger(JsonRpcClient.class.getName());

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
    private Hashtable<String, Request> mOutstandingRequests =
            new Hashtable<String, Request>();

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
     * @param mExceptionResolver the exceptionResolver to set
     */
    public void setExceptionResolver(ExceptionResolver mExceptionResolver) {
        this.mExceptionResolver = mExceptionResolver;
    }

    /**
     * Generate a new request id and return it
     * @return
     */
    private String generateId() {
        return mIdGenerator.nextLong()+"";
    }

    /**
     * Invoke the method specified via the given connection
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
        String id = generateId();
        ObjectNode requestNode = createRequest(methodName, arguments, id);
        Request request = new Request(id, requestNode, connection);
        mOutstandingRequests.put(id, request);
        try {
            connection.sendRequest(requestNode);
            result = request.waitForResponse(returnType);
        } catch (Throwable t) {
            request.handleException(t);
            throw t;
        }
        return result;
	}

    /**
     * Invoke the method specified via the given connection
     * @param methodName
     * @param arguments
     * @param connection
     */
	public void invokeNotification(String methodName, Object arguments, JsonRpcConnection connection)
        throws Throwable {
		ObjectNode requestNode = createRequest(methodName, arguments, null);
        Request request = new Request(null, requestNode, connection);
        try {
		    connection.sendRequest(requestNode);
        } catch (Throwable t) {
            request.handleException(t);
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
		if(idNode != null && idNode.isTextual()) {
			String id = idNode.asText();
			Request req = mOutstandingRequests.get(id);
			if(req != null && req.getConnection() == connection) {
				req.handleResponse(response);
                return true;
			}
		}
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
	
	private class Request {
		Lock mLock;
		Condition mCondition;
		String mId;
        Throwable mException;
		ObjectNode mRequest;
		ObjectNode mResponse;
		JsonRpcConnection mConnection;
		public Request(String id, ObjectNode request, JsonRpcConnection connection) {
			mLock = new ReentrantLock();
			mCondition = mLock.newCondition();
			mId = id;
			mRequest = request;
			mResponse = null;
			mConnection = connection;
		}
        public String getId() {
            return mId;
        }
		public JsonRpcConnection getConnection() {
			return mConnection;
		}
        private boolean isDone() {
            return mResponse != null || mException != null;
        }
		public void handleException(Throwable exception) {
			mLock.lock();
			try {
                if(!isDone()) {
                    mException = exception;
                    mCondition.signalAll();
                }
			} finally {
				mLock.unlock();
			}
		}
		public void handleResponse(ObjectNode response) {
			mLock.lock();
			try {
				if(!isDone()) {
					mResponse = response;
					mCondition.signalAll();
				}
			} finally {
				mLock.unlock();
			}
		}
		public Object waitForResponse(Type returnType) throws Throwable {
			mLock.lock();
			try {
				// wait until done or timeout is reached
                long timeout = System.currentTimeMillis() + mRequestTimeout;
                while(!isDone()) {
                    long timeLeft = timeout - System.currentTimeMillis();
                    if(timeLeft <= 0) {
                        throw new TimeoutException("JSON-RPC timeout");
                    }
					try {
						mCondition.await(timeLeft, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
					}
				}
				
				// detect rpc failures
				if (mException != null) {
                    throw new RuntimeException("JSON-RPC failure", mException);
				}
				
				// detect errors
				if (mResponse.has("error")
					&& mResponse.get("error")!=null
					&& !mResponse.get("error").isNull()) {

					// resolve and throw the exception
					if (mExceptionResolver == null) {
						throw DefaultExceptionResolver.INSTANCE.resolveException(mResponse);
					} else {
						throw mExceptionResolver.resolveException(mResponse);
					}
				}
				
				// convert it to a return object
				if (mResponse.has("result")
					&& !mResponse.get("result").isNull()
					&& mResponse.get("result")!=null) {
					if (returnType==null) {
						LOG.warning(
                                "Server returned result but returnType is null");
						return null;
					}
					
					JsonParser returnJsonParser = mMapper.treeAsTokens(mResponse.get("result"));
					JavaType returnJavaType = TypeFactory.defaultInstance().constructType(returnType);
					
					return mMapper.readValue(returnJsonParser, returnJavaType);
				}
				
				return null;
			} finally {
				mLock.unlock();
			}
		}
	}

}
