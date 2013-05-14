package better.jsonrpc.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class JsonRpcExecutorConnection extends JsonRpcLocalConnection {

    public static List<JsonRpcExecutorConnection> createExecutorConnectionPair() {
        return createExecutorConnectionPair(Executors.newCachedThreadPool());
    }

    /**
     * Create a local connected pair of executor connections
     *
     * The connections can be used immediately.
     *
     * @return list containing exactly 2 connections
     */
    public static List<JsonRpcExecutorConnection> createExecutorConnectionPair(Executor executor) {
        List<JsonRpcExecutorConnection> res = new ArrayList<JsonRpcExecutorConnection>(2);
        JsonRpcExecutorConnection a = new JsonRpcExecutorConnection(executor);
        JsonRpcExecutorConnection b = new JsonRpcExecutorConnection(executor);
        a.setOtherConnection(b);
        b.setOtherConnection(a);
        res.add(0, a);
        res.add(1, b);
        return res;
    }

    Executor mExecutor;

    public JsonRpcExecutorConnection() {
        mExecutor = Executors.newCachedThreadPool();
    }

    public JsonRpcExecutorConnection(Executor executor) {
        mExecutor = executor;
    }

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