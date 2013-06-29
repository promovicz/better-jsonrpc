package better.jsonrpc.websocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import better.jsonrpc.core.JsonRpcConnection;

public class JsonRpcWsConnection extends JsonRpcConnection
        implements WebSocket, WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {
	
	/** Currently active websocket connection */
	private Connection mConnection;

    /** Max idle time for the connection */
    private int mMaxIdleTime = 300 * 1000;

    /** Max test message size */
    private int mMaxTextMessageSize = 1 << 16;

    /** Max binary message size */
    private int mMaxBinaryMessageSize = 1 << 16;

    /** Whether to accept binary messages */
    private boolean mAcceptBinaryMessages = true;

    /** Whether to accept text messages */
    private boolean mAcceptTextMessages = true;

    /** Whether to send binary messages (text is the default) */
    private boolean mSendBinaryMessages = false;
	
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

    public boolean isAcceptBinaryMessages() {
        return mAcceptBinaryMessages;
    }

    public void setAcceptBinaryMessages(boolean acceptBinaryMessages) {
        this.mAcceptBinaryMessages = acceptBinaryMessages;
    }

    public boolean isAcceptTextMessages() {
        return mAcceptTextMessages;
    }

    public void setAcceptTextMessages(boolean acceptTextMessages) {
        this.mAcceptTextMessages = acceptTextMessages;
    }

    public boolean isSendBinaryMessages() {
        return mSendBinaryMessages;
    }

    public void setSendBinaryMessages(boolean sendBinaryMessages) {
        this.mSendBinaryMessages = sendBinaryMessages;
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
		if(mConnection != null && mConnection.isOpen()) {
			mConnection.sendMessage(data);
		}
	}

    public void transmit(byte[] data, int offset, int length) throws IOException {
        if(mConnection != null && mConnection.isOpen()) {
            mConnection.sendMessage(data, offset, length);
        }
    }

    public void transmit(JsonNode node) throws IOException {
        if(LOG.isTraceEnabled()) {
            LOG.trace("[" + mConnectionId + "] transmitting \"" + node.toString() + "\"");
        }
        if(mSendBinaryMessages) {
            byte[] data = getMapper().writeValueAsBytes(node);
            transmit(data, 0, data.length);
        } else {
            String data = getMapper().writeValueAsString(node);
            transmit(data);
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

    private void onMessage(JsonNode message) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("[" + mConnectionId + "] received \"" + message.toString() + "\"");
        }
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
    }
	
	@Override
	public void onMessage(String data) {
        if(mAcceptTextMessages) {
            try {
                onMessage(getMapper().readTree(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

    @Override
    public void onMessage(byte[] data, int offset, int length) {
        if(mAcceptBinaryMessages) {
            InputStream is = new ByteArrayInputStream(data, offset, length);
            try {
                onMessage(getMapper().readTree(is));
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        transmit(request);
	}

	@Override
	public void sendResponse(ObjectNode response) throws IOException {
        transmit(response);
	}
	
	@Override
	public void sendNotification(ObjectNode notification) throws IOException {
		transmit(notification);
	}
	
}
