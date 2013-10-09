package better.jsonrpc.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class JsonRpcHttpServlet extends HttpServlet {
	
	JsonRpcServer mServer;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		mServer = new JsonRpcServer(getJsonRpcInterfaces());
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JsonRpcHttp.handle(mServer, getJsonRpcHandler(request), request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JsonRpcHttp.handle(mServer, getJsonRpcHandler(request), request, response);
	}
	
	protected abstract Class<?>[] getJsonRpcInterfaces();
	
	protected abstract Object getJsonRpcHandler(HttpServletRequest request);

}
