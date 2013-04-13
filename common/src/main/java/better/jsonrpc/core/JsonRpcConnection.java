package better.jsonrpc.core;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
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
	protected static final Logger log = Logger.getLogger(JsonRpcConnection.class.getSimpleName());

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
     *
     * XXX this should be part of the server instance
     */
	Object mHandler;


    /** Connection listeners */
	Vector<Listener> mListeners = new Vector<Listener>();

    /** Object mapper to be used for mapping JSON */
    protected ObjectMapper mMapper;


    /**
     * Main constructor
     *
     * @param mapper to be used for mapping JSON
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
	
	public JsonRpcClient getClient() {
		if(mClient == null) {
			mClient = new JsonRpcClient(mMapper);
		}
		return mClient;
	}
	
	public void setHandler(Object handler) {
		mHandler = handler;
	}
	
	public void setServer(JsonRpcServer server) {
		mServer = server;
	}
	
	public JsonRpcServer getServer(Class<?> remoteInterface) {
		return mServer;
	}
	
	public void addListener(Listener l) {
		mListeners.add(l);
	}
	
	public void removeListener(Listener l) {
		mListeners.remove(l);
	}
	
	abstract public boolean isConnected();
	abstract public void sendRequest(ObjectNode request);
	abstract public void sendResponse(ObjectNode response);
	abstract public void sendNotification(ObjectNode notification);
	
	public void handleRequest(ObjectNode request) {
		if(mServer != null) {
			mServer.handleRequest(mHandler, request, this);
		}
	}
	
	public void handleResponse(ObjectNode response) {
		if(mClient != null) {
			mClient.handleResponse(response, this);
		}
	}
	
	public void handleNotification(ObjectNode notification) {
		if(mServer != null) {
			mServer.handleRequest(mHandler, notification, this);
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
