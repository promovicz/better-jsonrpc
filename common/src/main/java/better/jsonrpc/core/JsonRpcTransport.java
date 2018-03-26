package better.jsonrpc.core;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.client.JsonRpcClientRequest;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.util.ProxyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JSON-RPC transport
 *
 * This is our protocol abstraction layer, separating
 * JSON-RPC aspects from lower level protocol details.
 *
 * A transport can be attached to one server and one client each.
 * The client may send requests and notifications and will receive responses.
 * The server receives requests and notifications and creates responses.
 *
 */
public abstract class JsonRpcTransport {

    /** Global logger, may be used by subclasses */
	protected static final Logger LOG = LoggerFactory.getLogger(JsonRpcTransport.class);

    /** Global counter for connection IDs */
	private static final AtomicInteger sTransportIdCounter = new AtomicInteger();


    /** Automatically assign connections an ID for debugging and other purposes */
	protected final int mTransportId = sTransportIdCounter.incrementAndGet();


    /** Object mapper to be used for this transport */
    ObjectMapper mMapper;

    /** Server instance attached to this transport */
	JsonRpcServer mServer;

    /** Client instance attached to this transport */
	JsonRpcClient mClient;

    /** Handler instance for this transport */
	Object mServerHandler;


    /** Main constructor */
    public JsonRpcTransport(ObjectMapper mapper) {
        mMapper = mapper;
    }

    /** @return the ID of this transport */
    public int getTransportId() {
        return mTransportId;
    }

    /** @return the object mapper for this connection */
    public ObjectMapper getMapper() {
        return mMapper;
    }

    /** @return true if this connection has a client bound to it */
    public boolean isClient() {
        return mClient != null;
    }

    /** @return  the client if there is one (throws otherwise!) */
    public JsonRpcClient getClient() {
        if(mClient == null) {
            throw new RuntimeException("Connection not configured for client mode");
        }
        return mClient;
    }

    /** Bind the given client to this connection */
    public void bindClient(JsonRpcClient client) {
        if(mClient != null) {
            throw new RuntimeException("Connection already has a client");
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("[" + mTransportId + "] binding client");
        }

        mClient = client;

        mClient.bindConnection(this);
    }

    /** Create and return a client proxy */
    public <T> T makeProxy(Class<T> clazz) {
        return ProxyUtil.createClientProxy(clazz.getClassLoader(), clazz, this);
    }


    /** @return true if this connection has a server bound to it */
    public boolean isServer() {
        return mServer != null;
    }

    /** @return the server if there is one (throws otherwise!) */
    public JsonRpcServer getServer() {
        if(mServer == null) {
            throw new RuntimeException("Connection not configured for server mode");
        }
        return mServer;
    }

    /** Bind the given server to this connection */
    public void bindServer(JsonRpcServer server, Object handler) {
        if(mServer != null) {
            throw new RuntimeException("Connection already has a server");
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("[" + mTransportId + "] binding server");
        }

        mServer = server;
        mServerHandler = handler;
    }


    /** Sends a request through the connection */
	abstract public void sendRequest(JsonRpcClientRequest request) throws IOException;
    /** Sends a notification through the connection */
	abstract public void sendNotification(JsonRpcClientRequest request) throws IOException;

    /** Sends a response through the connection */
    abstract public void sendResponse(ObjectNode response) throws IOException;

    /** Dispatch an incoming request (for subclasses to call) */
	protected void handleRequest(ObjectNode request) {
		if(mServer != null) {
            try {
                mServer.handleRequest(mServerHandler, request, this);
            } catch (Throwable throwable) {
                LOG.error("Exception handling request", throwable);
            }
        }
	}

    /** Dispatch an incoming response (for subclasses to call) */
	protected void handleResponse(ObjectNode response) {
		if(mClient != null) {
            try {
			mClient.handleResponse(response, this);
            } catch (Throwable throwable) {
                LOG.error("Exception handling response", throwable);
            }
		}
	}

    /** Dispatch an incoming notification (for subclasses to call) */
	protected void handleNotification(ObjectNode notification) {
		if(mServer != null) {
            try {
                mServer.handleRequest(mServerHandler, notification, this);
            } catch (Throwable throwable) {
                LOG.error("Exception handling notification", throwable);
            }
        }
	}
	
}
