package better.jsonrpc.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JsonRpcWsClient extends JsonRpcWsTransport
        implements WebSocket, WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

	/** URI for the service used */
	private URI mServiceUri;
    private String mServiceProtocol;

	/** Websocket client */
	private WebSocketClient mClient;

    public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClient client, ObjectMapper mapper) {
        super(mapper);
        mServiceUri = serviceUri;
        mServiceProtocol = protocol;
        mClient = client;
    }

	public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClient client) {
		this(serviceUri, protocol, client, new ObjectMapper());
	}

    public JsonRpcWsClient(URI serviceUri, String protocol, WebSocketClientFactory clientFactory) {
        this(serviceUri, protocol, clientFactory.newWebSocketClient(), new ObjectMapper());
    }

    public JsonRpcWsClient(URI serviceUri, String protocol) {
        this(serviceUri, protocol, null, new ObjectMapper());
        WebSocketClientFactory clientFactory = new WebSocketClientFactory();
        try {
            clientFactory.start();
        } catch (Exception e) {
            LOG.error("Error creating WS client factory", e);
        }
        mClient = clientFactory.newWebSocketClient();
    }

    public WebSocketClient getWebSocketClient() {
        return mClient;
    }

    public URI getServiceUri() {
        return mServiceUri;
    }

    public void setServiceUri(URI serviceUri) {
        this.mServiceUri = serviceUri;
    }

    public String getServiceProtocol() {
        return mServiceProtocol;
    }

    public void setServiceProtocol(String serviceProtocol) {
        this.mServiceProtocol = serviceProtocol;
    }

    public void connect() throws IOException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("[" + mConnectionId + "] connecting");
        }
        mClient.setProtocol(mServiceProtocol);
		mClient.open(mServiceUri, this);
	}

    public void connect(long maxWait, TimeUnit maxWaitUnit)
            throws TimeoutException, IOException, InterruptedException
    {
        if(LOG.isDebugEnabled()) {
            LOG.debug("[" + mConnectionId + "] connecting with timeout of " + maxWait + " " + maxWaitUnit);
        }
        mClient.setProtocol(mServiceProtocol);
        mClient.open(mServiceUri, this, maxWait, maxWaitUnit);
    }

}
