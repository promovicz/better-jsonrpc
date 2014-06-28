package better.jsonrpc.servlet;

import better.jsonrpc.core.JsonRpcTransport;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

public class JsonRpcHttp {

    public static final int CONTENT_LENGTH_MAX = 1 << 16;

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_BSON = "application/bson";

    public static void doRequest(JsonRpcServer server, Object handler, ObjectMapper mapper,
                                 HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // check content length
        int contentLength = req.getContentLength();
        if(contentLength > CONTENT_LENGTH_MAX) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Content length must be <= " + CONTENT_LENGTH_MAX);
            return;
        }
        // determine stream depending on HTTP method
        String requestMethod = req.getMethod();
        InputStream requestStream;
        if(requestMethod.equals("POST")) {
            requestStream = req.getInputStream();
        } else if(requestMethod.equals("GET")) {
            requestStream = createInputStream(req);
        } else {
            throw new ServletException("Method " + requestMethod + " not supported");
        }
        // read the request
        ObjectNode request = mapper.readValue(requestStream, ObjectNode.class);
        // let the server process the request
        JsonRpcTransport transport = new JsonRpcHttpServletTransport(mapper, req, resp);
        server.handleRequest(handler, request, transport);
    }

    private static InputStream createInputStream(HttpServletRequest request)
            throws ServletException, IOException {
        // get components of request
        String method = request.getParameter("method");
        String id = request.getParameter("id");
        String params = request.getParameter("params");
        if(method == null || id == null || params == null) {
            throw new ServletException("Missing JSON-RPC parameters");
        }
        // decode parameters
        String decodedParams = URLDecoder.decode(
                new String(Base64.decode(params)), "UTF-8");
        // create request
        String message =
                new StringBuilder()
                .append("{ ")
                .append("\"id\": \"").append(id).append("\", ")
                .append("\"method\": \"").append(method).append("\", ")
                .append("\"params\": ").append(decodedParams)
                .append(" }")
                .toString();
        // turn it into a stream
        return new ByteArrayInputStream(message.getBytes());
    }

}
