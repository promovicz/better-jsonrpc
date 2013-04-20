package better.jsonrpc.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

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
			URI serviceUri) {
		super();
		
		mClientFactory = factory;
		mServiceUri = serviceUri;
		
		mClient = mClientFactory.newWebSocketClient();
	}

    public WebSocketClient getWebSocketClient() {
        return mClient;
    }

	public void connect() throws IOException {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info("[" + mConnectionId + "] connecting");
        }
		mClient.open(mServiceUri, this);
	}

    public void connect(long maxWait, TimeUnit maxWaitUnit)
            throws TimeoutException, IOException, InterruptedException
    {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info("[" + mConnectionId + "] connecting with timeout of " + maxWait + " " + maxWaitUnit);
        }
        mClient.open(mServiceUri, this, maxWait, maxWaitUnit);
    }

}
