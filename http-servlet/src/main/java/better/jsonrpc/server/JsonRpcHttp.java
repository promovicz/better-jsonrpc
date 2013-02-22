package better.jsonrpc.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import better.jsonrpc.util.Base64;

public class JsonRpcHttp {

	/**
	 * Returns parameters into an {@link InputStream} of JSON data.
	 *
	 * @param method the method
	 * @param id the id
	 * @param params the base64 encoded params
	 * @return the {@link InputStream}
	 * @throws IOException on error
	 */
	private static InputStream createInputStream(String method, String id, String params)
		throws IOException {

		// decode parameters
		String decodedParams = URLDecoder.decode(
			new String(Base64.decode(params)), "UTF-8");

		// create request
		String request = new StringBuilder()
			.append("{ ")
			.append("\"id\": \"").append(id).append("\", ")
			.append("\"method\": \"").append(method).append("\", ")
			.append("\"params\": ").append(decodedParams).append(" ")
			.append("}")
			.toString();

		// turn into InputStream
		return new ByteArrayInputStream(request.getBytes());
	}
	
	/**
	 * Handles a servlet request.
	 *
	 * @param request the {@link HttpServletRequest}
	 * @param response the {@link HttpServletResponse}
	 * @throws IOException on error
	 */
	public static void handle(
			JsonRpcServer server, Object handler,
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		// set response type
		response.setContentType(JsonRpcServer.JSONRPC_RESPONSE_CONTENT_TYPE);

		// setup streams
		InputStream input 	= null;
		OutputStream output	= response.getOutputStream();

		// POST
		if (request.getMethod().equals("POST")) {
			input = request.getInputStream();

		// GET
		} else if (request.getMethod().equals("GET")) {
			input = createInputStream(
				request.getParameter("method"),
				request.getParameter("id"),
				request.getParameter("params"));

		// invalid request
		} else {
			throw new IOException(
				"Invalid request method, only POST and GET is supported");
		}

		// service the request
		server.handle(handler, input, output);
	}
	
}
