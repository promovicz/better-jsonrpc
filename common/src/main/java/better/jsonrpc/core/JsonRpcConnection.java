package better.jsonrpc.core;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.server.JsonRpcServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	protected static final Logger LOG = Logger.getLogger(JsonRpcConnection.class.getSimpleName());

    /** Global counter for connection IDs */
	private static final AtomicInteger sConnectionIdCounter = new AtomicInteger();


    /** Automatically assign connections an ID for debugging and other purposes */
	protected final int mConnectionId = sConnectionIdCounter.incrementAndGet(); 


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
	public JsonRpcConnection() {
	}

    /**
     * Get the numeric local connection ID
     * @return
     */
    public int getConnectionId() {
        return mConnectionId;
    }


    public boolean isClient() {
        return mClient != null;
    }

    public JsonRpcClient getClient() {
        if(mClient == null) {
            throw new RuntimeException("Connection not configured for client mode");
        }
        return mClient;
    }

    public void bindClient(JsonRpcClient client) {
        if(mClient != null) {
            throw new RuntimeException("Connection already has a client");
        }

        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + mConnectionId + "] binding client");
        }

        mClient = client;

        mClient.bindConnection(this);
    }


    public boolean isServer() {
        return mServer != null;
    }

    public JsonRpcServer getServer() {
        if(mServer == null) {
            throw new RuntimeException("Connection not configured for server mode");
        }
        return mServer;
    }

    public void bindServer(JsonRpcServer server, Object handler) {
        if(mServer != null) {
            throw new RuntimeException("Connection already has a server");
        }

        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("[" + mConnectionId + "] binding server");
        }

        mServer = server;
        mServerHandler = handler;
    }


	public void addListener(Listener l) {
		mListeners.add(l);
	}
	
	public void removeListener(Listener l) {
		mListeners.remove(l);
	}
	
	abstract public boolean isConnected();
	abstract public void sendRequest(ObjectNode request) throws Exception;
	abstract public void sendResponse(ObjectNode response) throws Exception;
	abstract public void sendNotification(ObjectNode notification) throws Exception;
	
	public void handleRequest(ObjectNode request) {
		if(mServer != null) {
            try {
                mServer.handleRequest(mServerHandler, request, this);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
	}
	
	public void handleResponse(ObjectNode response) {
		if(mClient != null) {
			mClient.handleResponse(response, this);
		}
	}
	
	public void handleNotification(ObjectNode notification) {
		if(mServer != null) {
            try {
                mServer.handleRequest(mServerHandler, notification, this);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
	}
	
	public interface Listener {
		public void onOpen(JsonRpcConnection connection);
		public void onClose(JsonRpcConnection connection);
    }
	
	public void onOpen() {
		for(Listener l: mListeners) {
			l.onOpen(this);
		}
	}
	
	public void onClose() {
		for(Listener l: mListeners) {
			l.onClose(this);
		}
	}
	
}
