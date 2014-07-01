package better.jsonrpc.servlet;

import better.jsonrpc.core.JsonRpcTransport;
import better.jsonrpc.server.JsonRpcServer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.undercouch.bson4jackson.BsonFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class JsonRpcHttpServlet extends HttpServlet {

    ObjectMapper mJsonMapper;
    ObjectMapper mBsonMapper;

    /**
     * Initialize the servlet
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mJsonMapper = new ObjectMapper(new JsonFactory());
        mBsonMapper = new ObjectMapper(new BsonFactory());
    }

    /**
     * Handle POST requests
     *
     * This type of request will be processed as a normal JSON or BSON RPC request,
     * meaning that the body of the POST request will be used as an RPC request.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonRpcHttp.doRequest(getServer(req), getHandler(req), getMapper(req), req, resp);
    }

    /**
     * Handle GET requests
     *
     * This type of request will be handled as a compatibility request,
     * meaning that the request object will be constructed from HTTP
     * URL parameters.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonRpcHttp.doRequest(getServer(req), getHandler(req), getMapper(req), req, resp);
    }

    /**
     * Determine the object mapper for the request
     *
     * @param request to determine the mapper for
     * @return object mapper to be used
     * @throws ServletException when content type is not supported
     */
    protected ObjectMapper getMapper(HttpServletRequest request) throws ServletException {
        String contentType = request.getContentType();
        ObjectMapper mapper = null;
        if(contentType.equals(JsonRpcHttp.CONTENT_TYPE_JSON)) {
            mapper = mJsonMapper;
        }
        if(contentType.equals(JsonRpcHttp.CONTENT_TYPE_BSON)) {
            mapper = mBsonMapper;
        }
        if(mapper == null) {
            throw new ServletException("Unsupported content type for JSON-RPC: " + contentType);
        }
        return mapper;
    }

    /**
     * Determine the JSON-RPC server corresponding to the request
     *
     * @param request to determine the server for
     * @return server associated with the request
     */
    protected abstract JsonRpcServer getServer(HttpServletRequest request);

    /**
     * Determine the JSON-RPC handler object corresponding to the request
     *
     * @param request to determine the server for
     * @return server associated with the request
     */
    protected abstract Object getHandler(HttpServletRequest request);

}
