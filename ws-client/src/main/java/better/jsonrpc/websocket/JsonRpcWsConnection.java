package better.jsonrpc.websocket;

import java.io.IOException;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import better.jsonrpc.core.JsonRpcConnection;

public class JsonRpcWsConnection extends JsonRpcConnection
	implements WebSocket, OnTextMessage {
	
	/** Currently active websocket connection */
	private Connection mConnection;
	
	public JsonRpcWsConnection(ObjectMapper mapper) {
		super(mapper);
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
	
	public void transmit(String data) {
		log.fine("[" + mConnectionId + "] connection transmitting \"" + data + "\"");
		if(mConnection != null && mConnection.isOpen()) {
			try {
				mConnection.sendMessage(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onOpen(Connection connection) {
		log.fine("[" + mConnectionId + "] connection open");
		super.onOpen();
		mConnection = connection;
	}
	
	@Override
	public void onClose(int closeCode, String message) {
		log.fine("[" + mConnectionId + "] connection close " + closeCode + "/" + message);
		super.onClose();
		mConnection = null;
	}
	
	@Override
	public void onMessage(String data) {
		log.fine("[" + mConnectionId + "] connection received message \"" + data + "\"");
		try {
			JsonNode message = mMapper.readTree(data);
			if(message.isObject()) {
				ObjectNode messageObj = ObjectNode.class.cast(message);
				// requests and notifications
				if(messageObj.has("method") && messageObj.has("params")) {
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
	
	@Override
	public void sendRequest(ObjectNode request) {
		transmit(request.toString());
	}

	@Override
	public void sendResponse(ObjectNode response) {
		transmit(response.toString());
	}
	
	@Override
	public void sendNotification(ObjectNode notification) {
		transmit(notification.toString());
	}
	
}
