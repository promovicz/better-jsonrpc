package better.jsonrpc.core;

import better.jsonrpc.client.JsonRpcClientRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * In-process connections using executors
 *
 * These come in pairs and perform their work on a provided executor.
 *
 * JSON structures are passed directly.
 */
public class JsonRpcExecutorTransport extends JsonRpcTransport {

    /**
     * Create a local connected pair of connections
     *
     * The connections can be used immediately.
     *
     * @return list containing exactly 2 connections
     */
    public static List<JsonRpcExecutorTransport> createExecutorConnectionPair() {
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
    public static List<JsonRpcExecutorTransport> createExecutorConnectionPair(ObjectMapper mapper) {
        return createExecutorConnectionPair(mapper, Executors.newCachedThreadPool());
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
    public static List<JsonRpcExecutorTransport> createExecutorConnectionPair(ObjectMapper mapper, Executor executor) {
        List<JsonRpcExecutorTransport> res = new ArrayList<JsonRpcExecutorTransport>(2);
        JsonRpcExecutorTransport a = new JsonRpcExecutorTransport(mapper, executor);
        JsonRpcExecutorTransport b = new JsonRpcExecutorTransport(mapper, executor);
        a.setOtherConnection(b);
        b.setOtherConnection(a);
        res.add(0, a);
        res.add(1, b);
        return res;
    }

    /** Executor used to decouple the connection */
    private Executor mExecutor;

    /** The partner connection of this connection */
    private JsonRpcExecutorTransport mOtherConnection;

    /**
     * Main constructor
     *
     * @param mapper
     * @param executor
     */
    private JsonRpcExecutorTransport(ObjectMapper mapper, Executor executor) {
        super(mapper);
        mExecutor = executor;
    }

    /** @return the executor used to decouple this connection */
    public Executor getExecutor() {
        return mExecutor;
    }

    /** @return the partner connection of this connection */
    public JsonRpcExecutorTransport getOtherConnection() {
        return mOtherConnection;
    }

    /** @param otherConnection to use from now on */
    private void setOtherConnection(JsonRpcExecutorTransport otherConnection) {
        this.mOtherConnection = otherConnection;
    }

    /** {@inheritDoc} */
    @Override
    public void sendRequest(final JsonRpcClientRequest request) throws IOException {
        mExecutor.execute(() ->
                mOtherConnection.handleRequest(request.getRequest()));
    }

    /** {@inheritDoc} */
    @Override
    public void sendNotification(final JsonRpcClientRequest notification) throws IOException {
        mExecutor.execute(() ->
                mOtherConnection.handleNotification(notification.getRequest()));
    }

    /** {@inheritDoc} */
    @Override
    public void sendResponse(final ObjectNode response) throws IOException {
        mExecutor.execute(() ->
                mOtherConnection.handleResponse(response));
    }

}
