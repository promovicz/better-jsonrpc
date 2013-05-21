package better.jsonrpc.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Local connections that pass JSON structures directly
 *
 * Intended for testing.
 */
public class JsonRpcLocalConnection extends JsonRpcConnection {

    /**
     * Create a local connected pair of connections
     *
     * The connections can be used immediately.
     *
     * @return list containing exactly 2 connections
     */
    public static List<JsonRpcLocalConnection> createConnectedPair() {
        return createConnectedPair(new ObjectMapper());
    }

    /**
     * Create a local connected pair of connections
     *
     * The connections can be used immediately.
     *
     * @param mapper to be used for the connections
     * @return list containing exactly 2 connections
     */
    public static List<JsonRpcLocalConnection> createConnectedPair(ObjectMapper mapper) {
        List<JsonRpcLocalConnection> res = new ArrayList<JsonRpcLocalConnection>(2);
        JsonRpcLocalConnection a = new JsonRpcLocalConnection(mapper);
        JsonRpcLocalConnection b = new JsonRpcLocalConnection(mapper);
        a.setOtherConnection(b);
        b.setOtherConnection(a);
        res.add(0, a);
        res.add(1, b);
        return res;
    }

    /** The partner connection of this connection */
    JsonRpcLocalConnection mOtherConnection;

    /**
     * Main constructor
     *
     * @param mapper to be used for this connection
     */
    public JsonRpcLocalConnection(ObjectMapper mapper) {
        super(mapper);
    }

    /** @return the partner connection of this connection */
    public JsonRpcLocalConnection getOtherConnection() {
        return mOtherConnection;
    }

    /** @param otherConnection to use from now on */
    public void setOtherConnection(JsonRpcLocalConnection otherConnection) {
        this.mOtherConnection = otherConnection;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConnected() {
        return mOtherConnection != null;
    }

    /** {@inheritDoc} */
    @Override
    public void sendRequest(ObjectNode request) throws Exception {
        mOtherConnection.handleRequest(request);
    }

    /** {@inheritDoc} */
    @Override
    public void sendResponse(ObjectNode response) throws Exception {
        mOtherConnection.handleResponse(response);
    }

    /** {@inheritDoc} */
    @Override
    public void sendNotification(ObjectNode notification) throws Exception {
        mOtherConnection.handleNotification(notification);
    }

}
