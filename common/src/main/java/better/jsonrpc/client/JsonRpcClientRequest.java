package better.jsonrpc.client;

import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.exceptions.DefaultExceptionResolver;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JsonRpcClientRequest {
    JsonRpcClient mClient;
    Lock mLock;
    Condition mCondition;
    String mId;
    boolean mDisconnected;
    Throwable mException;
    ObjectNode mRequest;
    ObjectNode mResponse;
    JsonRpcConnection mConnection;

    public JsonRpcClientRequest(String id, ObjectNode request, JsonRpcConnection connection) {
        mLock = new ReentrantLock();
        mCondition = mLock.newCondition();
        mId = id;
        mRequest = request;
        mResponse = null;
        mConnection = connection;
        mClient = connection.getClient();
    }

    public String getId() {
        return mId;
    }

    public JsonRpcConnection getConnection() {
        return mConnection;
    }

    private boolean isDone() {
        return mDisconnected || mResponse != null || mException != null;
    }

    public void handleDisconnect() {
        mLock.lock();
        try {
            if (!isDone()) {
                mDisconnected = true;
            }
        } finally {
            mLock.unlock();
        }
    }

    public void handleException(Throwable exception) {
        mLock.lock();
        try {
            if (!isDone()) {
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
            if (!isDone()) {
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
            long timeout = System.currentTimeMillis() + mClient.getRequestTimeout();
            while (!isDone()) {
                long timeLeft = timeout - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    throw new TimeoutException("JSON-RPC timeout");
                }
                try {
                    mCondition.await(timeLeft, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                }
            }

            // throw if we got disconnected
            if (mDisconnected) {
                throw new RuntimeException("JSON-RPC disconnect");
            }

            // detect rpc failures
            if (mException != null) {
                throw new RuntimeException("JSON-RPC failure", mException);
            }

            // detect errors
            if (mResponse.has("error")
                    && mResponse.get("error") != null
                    && !mResponse.get("error").isNull()) {

                // resolve and throw the exception
                if (mClient.getExceptionResolver() == null) {
                    throw DefaultExceptionResolver.INSTANCE.resolveException(mResponse);
                } else {
                    throw mClient.getExceptionResolver().resolveException(mResponse);
                }
            }

            // convert it to a return object
            if (mResponse.has("result")
                    && !mResponse.get("result").isNull()
                    && mResponse.get("result") != null) {
                if (returnType == null) {
                    // XXX warn
                    return null;
                }

                ObjectMapper mapper = mClient.getObjectMapper();

                JsonParser returnJsonParser = mapper.treeAsTokens(mResponse.get("result"));
                JavaType returnJavaType = TypeFactory.defaultInstance().constructType(returnType);

                return mapper.readValue(returnJsonParser, returnJavaType);
            }
        } finally {
            mLock.unlock();
        }
        return null;
    }

}