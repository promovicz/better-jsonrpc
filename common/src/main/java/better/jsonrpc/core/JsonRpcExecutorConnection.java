package better.jsonrpc.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A variant of local connections using executors
 */
public class JsonRpcExecutorConnection extends JsonRpcLocalConnection {

    /**
     * Create a local connected pair of connections
     *
     * The connections can be used immediately.
     *
     * @return list containing exactly 2 connections
     */
    public static List<JsonRpcExecutorConnection> createExecutorConnectionPair() {
        return createExecutorConnectionPair(new ObjectMapper());
    }

    /**
     * Create a local connected pair of connections
     *
     * The connections can be used immediately.
     *
     * @param mapper to be used for this connection
     * @return list containing exactly 2 connections
     */
    public static List<JsonRpcExecutorConnection> createExecutorConnectionPair(ObjectMapper mapper) {
        return createExecutorConnectionPair(mapper, Executors.newCachedThreadPool());
    }

    /**
     * Create a local connected pair of executor connections
     *
     * The connections can be used immediately.
     *
     * @param executor to be used for decoupling the connnections
     * @return list containing exactly 2 connections
     */
    public static List<JsonRpcExecutorConnection> createExecutorConnectionPair(Executor executor) {
        return createExecutorConnectionPair(new ObjectMapper(), executor);
    }

    /**
     * Create a local connected pair of executor connections
     *
     * The connections can be used immediately.
     *
     * @param mapper to be used for this connection
     * @param executor to be used for decoupling the connnections
     * @return list containing exactly 2 connections
     */
    public static List<JsonRpcExecutorConnection> createExecutorConnectionPair(ObjectMapper mapper, Executor executor) {
        List<JsonRpcExecutorConnection> res = new ArrayList<JsonRpcExecutorConnection>(2);
        JsonRpcExecutorConnection a = new JsonRpcExecutorConnection(mapper, executor);
        JsonRpcExecutorConnection b = new JsonRpcExecutorConnection(mapper, executor);
        a.setOtherConnection(b);
        b.setOtherConnection(a);
        res.add(0, a);
        res.add(1, b);
        return res;
    }

    /** Executor used to decouple the connection */
    Executor mExecutor;

    /**
     * Main constructor
     *
     * @param mapper
     * @param executor
     */
    public JsonRpcExecutorConnection(ObjectMapper mapper, Executor executor) {
        super(mapper);
        mExecutor = executor;
    }

    /** @return the executor used to decouple this connection */
    public Executor getExecutor() {
        return mExecutor;
    }

    /** {@inheritDoc} */
    @Override
    public void sendRequest(final ObjectNode request) throws Exception {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonRpcExecutorConnection.super.sendRequest(request);
                } catch (Exception e) {
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void sendResponse(final ObjectNode response) throws Exception {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonRpcExecutorConnection.super.sendResponse(response);
                } catch (Exception e) {
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void sendNotification(final ObjectNode notification) throws Exception {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonRpcExecutorConnection.super.sendNotification(notification);
                } catch (Exception e) {
                }
            }
        });
    }
}
