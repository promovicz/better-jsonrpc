package better.jsonrpc.client;

import better.jsonrpc.core.JsonRpcTransport;
import better.jsonrpc.exception.JsonRpcDisconnect;
import better.jsonrpc.exception.JsonRpcInterrupted;
import better.jsonrpc.exception.JsonRpcProtocolError;
import better.jsonrpc.exception.JsonRpcTimeout;
import better.jsonrpc.exceptions.DefaultExceptionResolver;
import better.jsonrpc.exceptions.JavaExceptionResolver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JSON-RPC client requests
 *
 * These are used to keep track of outstanding requests in the client.
 *
 * They therefore also contain a considerable part of call logic.
 *
 */
public class JsonRpcClientRequest {

    /** Connection used for the request */
    JsonRpcTransport mConnection;
    /** Client tracking this request */
    JsonRpcClient mClient;

    /** Lock for request state */
    Lock mLock;
    /** Condition on mLock */
    Condition mCondition;
    /** Request id */
    String mId;
    /** Local exceptions */
    Throwable mLocalException;
    /** Remote exceptions (translated from error) */
    Throwable mRemoteException;
    /** The return value for the caller */
    Object mReturn;
    /** JSON request */
    ObjectNode mRequest;
    /** JSON response */
    ObjectNode mResponse;

    /** Constructs a client request */
    public JsonRpcClientRequest(String id, ObjectNode request, JsonRpcTransport connection) {
        mLock = new ReentrantLock();
        mCondition = mLock.newCondition();
        mId = id;
        mRequest = request;
        mResponse = null;
        mConnection = connection;
        mClient = connection.getClient();
    }

    /** Returns the request id of this request */
    public String getId() {
        return mId;
    }

    /** Returns the connection used */
    public JsonRpcTransport getConnection() {
        return mConnection;
    }

    /** Returns the client this request is for */
    public JsonRpcClient getClient() {
        return mClient;
    }

    /** Returns true if this request is done */
    private boolean isDone() {
        return mResponse != null || mLocalException != null;
    }

    /** Get the JSON request object */
    public ObjectNode getRequest() {
        return mRequest;
    }

    /** Get the JSON response object */
    public ObjectNode getResponse() {
        return mResponse;
    }

    /** Should be called when underlying connection fails */
    public void handleDisconnect() {
        handleLocalException(new JsonRpcDisconnect());
    }

    /** Should be called when caller times out */
    public void handleTimeout() {
        handleLocalException(new JsonRpcTimeout());
    }

    /** Should be called when caller got interrupted */
    public void handleInterrupted() {
        handleLocalException(new JsonRpcInterrupted());
    }

    /** Should be called on IO errors, timeouts and other such local abort causes */
    public void handleLocalException(Throwable exception) {
        mLock.lock();
        try {
            if (!isDone()) {
                mLocalException = exception;
                mCondition.signalAll();
            }
        } finally {
            mLock.unlock();
        }
    }

    /** Should be called when a matching response has been received */
    public void handleResponse(ObjectNode response) {
        mLock.lock();
        try {
            if (!isDone()) {
                mResponse = response;
                mCondition.signalAll();
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Wait for this request to finish and take appropriate action
     *
     * This will throw exceptions as resolved by the error resolver.
     *
     * If none occur, the return value of the RPC call is returned.
     *
     * This call will block, limited by the request timeout.
     */
    public boolean waitForCompletion() throws InterruptedException, TimeoutException {
        mLock.lock();
        try {
            // wait until done or timeout is reached
            long timeout = System.currentTimeMillis() + mClient.getRequestTimeout();
            while (!isDone()) {
                // recompute time left
                long timeLeft = timeout - System.currentTimeMillis();
                // throw on timeout
                if (timeLeft <= 0) {
                    throw new TimeoutException();
                }
                // wait for state changes
                mCondition.await(timeLeft, TimeUnit.MILLISECONDS);
            }
            return mResponse != null;
        } finally {
            mLock.unlock();
        }
    }

    public void processResponse(Type returnType) throws IOException {
        if (mResponse.has("result")) {
            // get the object mapper for conversion
            ObjectMapper mapper = mConnection.getMapper();
            // create a parser for the result
            JsonParser returnJsonParser = mapper.treeAsTokens(mResponse.get("result"));
            // determine type to convert to
            JavaType returnJavaType = TypeFactory.defaultInstance().constructType(returnType);
            // parse, convert and return
            mReturn = mapper.readValue(returnJsonParser, returnJavaType);
        } else if (mResponse.has("error")
                && mResponse.get("error") != null
                && !mResponse.get("error").isNull()) {
            // resolve the exception
            if (mClient.getExceptionResolver() == null) {
                mRemoteException = DefaultExceptionResolver.INSTANCE.resolveException(mResponse);
            } else {
                mRemoteException = mClient.getExceptionResolver().resolveException(mResponse);
            }
        } else {
            mLocalException = new JsonRpcProtocolError("Invalid response (neither result nor error)");
        }
    }

    public Object throwOrReturn() throws Throwable {
        if (mLocalException != null) {
            throw mLocalException;
        } else if (mRemoteException != null) {
            throw mRemoteException;
        } else {
            return mReturn;
        }
    }

}