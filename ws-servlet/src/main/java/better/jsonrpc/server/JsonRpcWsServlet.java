package better.jsonrpc.server;

import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JsonRpcWsServlet extends WebSocketServlet {
	
	ObjectMapper mMapper;
	JsonRpcServer mServer;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		mMapper = new ObjectMapper();
		mServer = new JsonRpcServer(getJsonRpcInterfaces());
	}
	
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest pRequest, String pProtocol) {
		Object handler = getJsonRpcHandler(pRequest);
		return new JsonRpcWsConnection(mMapper, mServer, handler);
	}
	
	protected abstract Class<?>[] getJsonRpcInterfaces();
	
	protected abstract Object getJsonRpcHandler(HttpServletRequest request);

}
