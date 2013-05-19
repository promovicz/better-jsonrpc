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

public class JsonRpcWsClient extends JsonRpcWsConnection implements WebSocket, OnTextMessage {

	/** URI for the service used */
	private URI mServiceUri;
    private String mServiceProtocol;

	/** Websocket client */
	private WebSocketClient mClient;

	public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClient client) {
		super(new ObjectMapper());
        mServiceUri = serviceUri;
        mServiceProtocol = protocol;
        mClient = client;
	}

    public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClientFactory clientFactory) {
        super(new ObjectMapper());
        mServiceUri = serviceUri;
        mServiceProtocol = protocol;
        WebSocketClient client = clientFactory.newWebSocketClient();
        client.setProtocol(protocol);
        mClient = client;
    }

    public JsonRpcWsClient(URI serviceUri, String protocol) {
        super(new ObjectMapper());
        mServiceUri = serviceUri;
        mServiceProtocol = protocol;
        WebSocketClientFactory clientFactory = new WebSocketClientFactory();
        try {
            clientFactory.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        WebSocketClient client = clientFactory.newWebSocketClient();
        client.setProtocol(mServiceProtocol);
        mClient = client;
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
