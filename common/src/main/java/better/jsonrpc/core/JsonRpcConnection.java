package better.jsonrpc.core;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.util.ProxyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Logger;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JSON-RPC connections
 *
 * These objects represent a single, possibly virtual,
 * connection between JSON-RPC speakers.
 *
 * Depending on the transport type, a connection can
 * be used as an RPC server and/or client. Notifications
 * may or may not be supported in either direction.
 *
 */
public abstract class JsonRpcConnection {

    /** Global logger, may be used by subclasses */
	protected static final Logger LOG = Logger.getLogger(JsonRpcConnection.class);

    /** Global counter for connection IDs */
	private static final AtomicInteger sConnectionIdCounter = new AtomicInteger();


    /** Automatically assign connections an ID for debugging and other purposes */
	protected final int mConnectionId = sConnectionIdCounter.incrementAndGet();


    /**
     * Object mapper to be used for this connection
     *
     * Both client and server should always use this mapper for this connection.
     */
    ObjectMapper mMapper;

    /**
     * Server instance attached to this client
     *
     * This object is responsible for handling requests and notifications.
     *
     * It will also send responses where appropriate.
     */
	JsonRpcServer mServer;

    /**
     * Client instance attached to this client
     *
     * This object is responsible for handling responses.
     *
     * It will send requests and notifications where appropriate.
     */
	JsonRpcClient mClient;

    /**
     * Handler instance for this client
     *
     * RPC calls will be dispatched to this through the server.
     */
	Object mServerHandler;


    /** Connection listeners */
	Vector<Listener> mListeners = new Vector<Listener>();


    /**
     * Main constructor
     */
    public JsonRpcConnection(ObjectMapper mapper) {
        mMapper = mapper;
    }

    /**
     * Get the numeric local connection ID
     * @return
     */
    public int getConnectionId() {
        return mConnectionId;
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
            LOG.debug("[" + mConnectionId + "] binding client");
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
            LOG.debug("[" + mConnectionId + "] binding server");
        }

        mServer = server;
        mServerHandler = handler;
    }


    /** Returns true if the connection is currently connected */
	abstract public boolean isConnected();
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
	public void handleRequest(ObjectNode request) {
		if(mServer != null) {
            try {
                mServer.handleRequest(mServerHandler, request, this);
            } catch (Throwable throwable) {
                LOG.error("Exception handling request", throwable);
            }
        }
	}

    /** Dispatch an incoming response (for subclasses to call) */
	public void handleResponse(ObjectNode response) {
		if(mClient != null) {
            try {
			mClient.handleResponse(response, this);
            } catch (Throwable throwable) {
                LOG.error("Exception handling response", throwable);
            }
		}
	}

    /** Dispatch an incoming notification (for subclasses to call) */
	public void handleNotification(ObjectNode notification) {
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
		public void onOpen(JsonRpcConnection connection);
		public void onClose(JsonRpcConnection connection);
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
