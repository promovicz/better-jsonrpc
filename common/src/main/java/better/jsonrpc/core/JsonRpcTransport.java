package better.jsonrpc.core;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.util.ProxyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;
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


    /** Transport listeners */
	Vector<Listener> mListeners = new Vector<Listener>();


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
	abstract public void sendRequest(ObjectNode request) throws Exception;
    /** Sends a response through the connection */
	abstract public void sendResponse(ObjectNode response) throws Exception;
    /** Sends a notification through the connection */
	abstract public void sendNotification(ObjectNode notification) throws Exception;

    /** Dispatch connection open event (for subclasses to call) */
    protected void onOpen() {
        for(Listener l: mListeners) {
            l.onOpen(this);
        }
    }

    /** Dispatch connection close event (for subclasses to call) */
    protected void onClose() {
        for(Listener l: mListeners) {
            l.onClose(this);
        }
    }

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

    /** Interface of connection state listeners */
	public interface Listener {
		public void onOpen(JsonRpcTransport connection);
		public void onClose(JsonRpcTransport connection);
    }

    /**
     * Add a connection state listener
     * @param l
     */
    public void addListener(Listener l) {
        mListeners.add(l);
    }

    /**
     * Remove the given connection state listener
     * @param l
     */
    public void removeListener(Listener l) {
        mListeners.remove(l);
    }
	
}
