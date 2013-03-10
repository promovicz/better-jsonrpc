package better.jsonrpc.websocket;

import java.io.IOException;
import java.net.URI;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonRpcWsClient extends JsonRpcWsConnection
	implements WebSocket, OnTextMessage {

	/** URI for the service used */
	private URI mServiceUri;
	
	/** Factory for websocket clients (currently used only once) */
	private WebSocketClientFactory mClientFactory;
	/** Websocket client */
	private WebSocketClient mClient;
	
	public JsonRpcWsClient(
			WebSocketClientFactory factory,
			ObjectMapper mapper,
			URI serviceUri) {
		super(mapper);
		
		mClientFactory = factory;
		mServiceUri = serviceUri;
		
		mClient = mClientFactory.newWebSocketClient();
	}
	
	public void connect() {
		try {
			mClient.open(mServiceUri, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
