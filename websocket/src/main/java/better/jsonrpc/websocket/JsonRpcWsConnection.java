package better.jsonrpc.websocket;

import java.io.IOException;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import better.jsonrpc.core.JsonRpcConnection;

public class JsonRpcWsConnection extends JsonRpcConnection implements WebSocket, OnTextMessage {
	
	/** Currently active websocket connection */
	private Connection mConnection;

    /** Max idle time for the connection */
    private int mMaxIdleTime = 300 * 1000;

    /** Max test message size */
    private int mMaxTextMessageSize = 1 << 16;

    /** Max binary message size */
    private int mMaxBinaryMessageSize = 1 << 16;
	
	public JsonRpcWsConnection(ObjectMapper mapper) {
		super(mapper);
	}

    public int getMaxIdleTime() {
        return mMaxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.mMaxIdleTime = maxIdleTime;
        applyConnectionParameters();
    }

    public int getMaxTextMessageSize() {
        return mMaxTextMessageSize;
    }

    public void setMaxTextMessageSize(int maxTextMessageSize) {
        this.mMaxTextMessageSize = maxTextMessageSize;
        applyConnectionParameters();
    }

    public int getMaxBinaryMessageSize() {
        return mMaxBinaryMessageSize;
    }

    public void setMaxBinaryMessageSize(int maxBinaryMessageSize) {
        this.mMaxBinaryMessageSize = maxBinaryMessageSize;
        applyConnectionParameters();
    }

    @Override
	public boolean isConnected() {
		return mConnection != null && mConnection.isOpen();
	}
	
	public void disconnect() {
		if(mConnection != null && mConnection.isOpen()) {
			mConnection.close();
		}
	}
	
	public void transmit(String data) throws IOException {
        if(LOG.isTraceEnabled()) {
		    LOG.trace("[" + mConnectionId + "] transmitting \"" + data + "\"");
        }
		if(mConnection != null && mConnection.isOpen()) {
			mConnection.sendMessage(data);
		}
	}

	@Override
	public void onOpen(Connection connection) {
        if(LOG.isDebugEnabled()) {
		    LOG.debug("[" + mConnectionId + "] connection open");
        }
		super.onOpen();
		mConnection = connection;
        applyConnectionParameters();
	}

	@Override
	public void onClose(int closeCode, String message) {
        if(LOG.isDebugEnabled()) {
		    LOG.debug("[" + mConnectionId + "] connection close " + closeCode + "/" + message);
        }
		super.onClose();
		mConnection = null;
	}
	
	@Override
	public void onMessage(String data) {
        if(LOG.isTraceEnabled()) {
		    LOG.trace("[" + mConnectionId + "] received \"" + data + "\"");
        }
		try {
			JsonNode message = getMapper().readTree(data);
			if(message.isObject()) {
				ObjectNode messageObj = ObjectNode.class.cast(message);
				
				// requests and notifications
				if(messageObj.has("method")) {
					if(messageObj.has("id")) {
						handleRequest(messageObj);
					} else {
						handleNotification(messageObj);
					}
				}
				// responses
				if(messageObj.has("result") || messageObj.has("error")) {
					if(messageObj.has("id")) {
						handleResponse(messageObj);
					}
				}
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

    private void applyConnectionParameters() {
        if(mConnection != null) {
            mConnection.setMaxIdleTime(mMaxIdleTime);
            mConnection.setMaxTextMessageSize(mMaxTextMessageSize);
            mConnection.setMaxBinaryMessageSize(mMaxBinaryMessageSize);
        }
    }
	
	@Override
	public void sendRequest(ObjectNode request) throws IOException {
		transmit(request.toString());
	}

	@Override
	public void sendResponse(ObjectNode response) throws IOException {
		transmit(response.toString());
	}
	
	@Override
	public void sendNotification(ObjectNode notification) throws IOException {
		transmit(notification.toString());
	}
	
}
