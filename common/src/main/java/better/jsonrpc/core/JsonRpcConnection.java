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

public abstract class JsonRpcConnection {

	protected static final Logger log = Logger.getLogger(JsonRpcConnection.class.getSimpleName());
	
	private static final AtomicInteger sConnectionIdCounter = new AtomicInteger();
	
	protected final int mConnectionId = sConnectionIdCounter.incrementAndGet(); 
	
	protected ObjectMapper mMapper;
		
	JsonRpcServer mServer;
	JsonRpcClient mClient;
	
	Object mHandler;
	
	Vector<Listener> mListeners = new Vector<Listener>();
	
	public JsonRpcConnection(ObjectMapper mapper) {
		mMapper = mapper;
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
