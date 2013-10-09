package better.jsonrpc.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class JsonRpcWsConnection implements WebSocket, OnTextMessage {

	private static final Logger LOG = Logger.getLogger(JsonRpcWsConnection.class.getSimpleName());
	
	private static final AtomicLong ID_COUNTER = new AtomicLong();
	
	long mId;
	Connection mConnection;
	ObjectMapper mMapper;
	JsonRpcServer mServer;
	Object mHandler;
	
	public JsonRpcWsConnection(ObjectMapper pMapper, JsonRpcServer pServer, Object pHandler) {
		mId = ID_COUNTER.incrementAndGet();
		mMapper = pMapper;
		mServer = pServer;
		mHandler = pHandler;
	}
	
	@Override
	public void onOpen(Connection pConnection) {
		LOG.info("connection #" + mId + " opened");
		mConnection = pConnection;
	}

	@Override
	public void onClose(int closeCode, String message) {
		LOG.info("connection #" + mId + " closed");
		mConnection = null;
	}

	@Override
	public void onMessage(String data) {
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			mServer.handle(mHandler, is, os);
			mConnection.sendMessage(os.toString());
			is.close();
			os.close();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
