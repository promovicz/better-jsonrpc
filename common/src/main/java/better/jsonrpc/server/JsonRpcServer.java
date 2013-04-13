package better.jsonrpc.server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import better.jsonrpc.annotations.JsonRpcParam;
import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.exceptions.AnnotationsErrorResolver;
import better.jsonrpc.exceptions.DefaultErrorResolver;
import better.jsonrpc.exceptions.ErrorResolver;
import better.jsonrpc.exceptions.MultipleErrorResolver;
import better.jsonrpc.util.ProtocolUtils;
import better.jsonrpc.util.ReflectionUtil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
/*
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
*/

/**
 * A JSON-RPC request server reads JSON-RPC requests from an
 * input stream and writes responses to an output stream.
 */
public class JsonRpcServer {

	protected static final Logger LOG = Logger.getLogger(JsonRpcServer.class.getName());

	public static final String JSONRPC_RESPONSE_CONTENT_TYPE = "application/json-rpc";

	public static final ErrorResolver DEFAULT_ERRROR_RESOLVER
		= new MultipleErrorResolver(AnnotationsErrorResolver.INSTANCE, DefaultErrorResolver.INSTANCE);

	private boolean backwardsComaptible		= true;
	private boolean rethrowExceptions 		= false;
	private boolean allowExtraParams 		= false;
	private boolean allowLessParams			= false;
	private ErrorResolver errorResolver	= null;
	private ObjectMapper mapper;
	private Class<?>[] remoteInterfaces;

	public JsonRpcServer(
		ObjectMapper mapper, Class<?>[] remoteInterfaces) {
		this.mapper				= mapper;
		this.remoteInterfaces	= remoteInterfaces;
	}
	
	public JsonRpcServer(
			ObjectMapper mapper, Class<?> remoteInterface) {
		this(mapper, new Class<?>[] { remoteInterface });
	}

	public JsonRpcServer(Class<?>[] remoteInterfaces) {
		this(new ObjectMapper(), remoteInterfaces);
	}
	
	public JsonRpcServer(Class<?> remoteInterface) {
		this(new ObjectMapper(), new Class<?>[] { remoteInterface });
	}

    /**
     * Returns the mapper used for mapping to and from JSON
     * @return
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

	/**
	 * Returns the handler's class or interfaces.
	 *
	 * @return the class
	 */
	private Class<?>[] getHandlerInterfaces() {
		return remoteInterfaces;
	}
	
	/**
	 * Handles the given {@link ObjectNode}.
	 *
	 * @param node the {@link JsonNode}
	 * @param ops the {@link OutputStream}
	 * @throws IOException on error
	 */
	public void handleRequest(Object handler, ObjectNode node, JsonRpcConnection connection) throws Throwable {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "Request: " + node.toString());
		}

		// validate request
		if (!backwardsComaptible && !node.has("jsonrpc") || !node.has("method")) {
			connection.sendResponse(
				ProtocolUtils.createErrorResponse(mapper,
					"2.0", "null", -32600, "Invalid Request", null));
			return;
		}

		// get nodes
		JsonNode jsonRpcNode	= node.get("jsonrpc");
		JsonNode methodNode		= node.get("method");
		JsonNode idNode 		= node.get("id");
		JsonNode paramsNode		= node.get("params");

		// get node values
		String version		= (jsonRpcNode!=null && !jsonRpcNode.isNull()) ? jsonRpcNode.asText() : "2.0";
		String methodName	= (methodNode!=null && !methodNode.isNull()) ? methodNode.asText() : null;
		Object id			= ProtocolUtils.parseId(idNode);
		
		// find methods
		Set<Method> methods = new HashSet<Method>();
		methods.addAll(ReflectionUtil.findMethods(getHandlerInterfaces(), methodName));
		if (methods.isEmpty()) {
			if(id != null) {
				connection.sendResponse(
					ProtocolUtils.createErrorResponse(
						mapper, version, id, -32601, "Method not found", null));
			}
			return;
		}
		
		// choose a method
		MethodAndArgs methodArgs = findBestMethodByParamsNode(methods, paramsNode);
		if (methodArgs==null) {
			if(id != null) {
				connection.sendResponse(
					ProtocolUtils.createErrorResponse(
						mapper, version, id, -32602, "Invalid method parameters", null));
			}
			return;
		}
		
		// invoke the method
		JsonNode result = null;
		Throwable thrown = null;
		try {
			result = invoke(handler, methodArgs.method, methodArgs.arguments);
		} catch (Throwable e) {
			thrown = e;
		}
		
		// log errors
		if(thrown != null) {
			if (LOG.isLoggable(Level.SEVERE)) {
				LOG.log(Level.SEVERE, "Error in JSON-RPC Service", thrown);
			}
		}
		
		// build response if not a notification
		if(id != null) {
			ObjectNode response = null;
			
			if(thrown == null) {
				response = ProtocolUtils.createSuccessResponse(
						mapper, version, id, result);
			} else {
				ErrorResolver.JsonError error = resolveError(thrown, methodArgs);
				response = ProtocolUtils.createErrorResponse(
						mapper, version, id,
						error.getCode(), error.getMessage(), error.getData());
			}
			
			connection.sendResponse(response);
		}
		
		// rethrow if applicable
		if(thrown != null && rethrowExceptions) {
			throw new RuntimeException(thrown);
		}
	}
	
	private ErrorResolver.JsonError resolveError(Throwable thrown, MethodAndArgs methodArgs) {
		// attempt to resolve the error
		ErrorResolver.JsonError error = null;
		
		// get cause of exception
		Throwable e = thrown;
		if (InvocationTargetException.class.isInstance(e)) {
			e = InvocationTargetException.class.cast(e).getTargetException();
		}

		// resolve error
		if (errorResolver!=null) {
			error = errorResolver.resolveError(
				e, methodArgs.method, methodArgs.arguments);
		} else {
			error = DEFAULT_ERRROR_RESOLVER.resolveError(
				e, methodArgs.method, methodArgs.arguments);
		}

		// make sure we have a JsonError
		if (error==null) {
			error = new ErrorResolver.JsonError(
				0, e.getMessage(), e.getClass().getName());
		}
		
		return error;
	}

	/**
	 * Invokes the given method on the {@code handler} passing
	 * the given params (after converting them to beans\objects)
	 * to it.
	 *
	 * @param m the method to invoke
	 * @param params the params to pass to the method
	 * @return the return value (or null if no return)
	 * @throws IOException on error
	 * @throws IllegalAccessException on error
	 * @throws InvocationTargetException on error
	 */
	protected JsonNode invoke(Object handler, Method m, List<JsonNode> params)
		throws IOException,
		IllegalAccessException,
		InvocationTargetException {

		// debug log
		if (LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "Invoking method: "+m.getName());
		}

		// convert the parameters
		Object[] convertedParams = new Object[params.size()];
		Type[] parameterTypes = m.getGenericParameterTypes();
		
		for (int i=0; i<parameterTypes.length; i++) {
		    JsonParser paramJsonParser = mapper.treeAsTokens(params.get(i));
		    JavaType paramJavaType = TypeFactory.defaultInstance().constructType(parameterTypes[i]);
			convertedParams[i] = mapper.readValue(paramJsonParser, paramJavaType);
		}

		// invoke the method
		Object result = m.invoke(handler, convertedParams);
		return (m.getGenericReturnType()!=null) ? mapper.valueToTree(result) : null;
	}

	/**
	 * Finds the {@link Method} from the supplied {@link Set} that
	 * best matches the rest of the arguments supplied and returns
	 * it as a {@link MethodAndArgs} class.
	 *
	 * @param methods the {@link Method}s
	 * @param paramsNode the {@link JsonNode} passed as the parameters
	 * @return the {@link MethodAndArgs}
	 */
	private MethodAndArgs findBestMethodByParamsNode(Set<Method> methods, JsonNode paramsNode) {

		// no parameters
		if (paramsNode==null || paramsNode.isNull()) {
			return findBestMethodUsingParamIndexes(methods, 0, null);

		// array parameters
		} else if (paramsNode.isArray()) {
			return findBestMethodUsingParamIndexes(methods, paramsNode.size(), ArrayNode.class.cast(paramsNode));

		// named parameters
		} else if (paramsNode.isObject()) {
			Set<String> fieldNames = new HashSet<String>();
			Iterator<String> itr=paramsNode.fieldNames();
			while (itr.hasNext()) {
				fieldNames.add(itr.next());
			}
			return findBestMethodUsingParamNames(methods, fieldNames, ObjectNode.class.cast(paramsNode));

		}

		// unknown params node type
		throw new IllegalArgumentException("Unknown params node type: "+paramsNode.toString());
	}

	/**
	 * Finds the {@link Method} from the supplied {@link Set} that
	 * best matches the rest of the arguments supplied and returns
	 * it as a {@link MethodAndArgs} class.
	 *
	 * @param methods the {@link Method}s
	 * @param paramCount the number of expect parameters
	 * @param paramNodes the parameters for matching types
	 * @return the {@link MethodAndArgs}
	 */
	private MethodAndArgs findBestMethodUsingParamIndexes(
		Set<Method> methods, int paramCount, ArrayNode paramNodes) {

		// get param count
		int numParams = paramNodes!=null && !paramNodes.isNull()
			? paramNodes.size() : 0;

		// determine param count
		int bestParamNumDiff		= Integer.MAX_VALUE;
		Set<Method> matchedMethods	= new HashSet<Method>();

		// check every method
		for (Method method : methods) {

			// get parameter types
			Class<?>[] paramTypes = method.getParameterTypes();
			int paramNumDiff = paramTypes.length-paramCount;

			// we've already found a better match
			if (Math.abs(paramNumDiff)>Math.abs(bestParamNumDiff)) {
				continue;

			// we don't allow extra params
			} else if (
				!allowExtraParams && paramNumDiff<0
				|| !allowLessParams && paramNumDiff>0) {
				continue;

			// check the parameters
			} else {
				if (Math.abs(paramNumDiff)<Math.abs(bestParamNumDiff)) {
					matchedMethods.clear();
				}
				matchedMethods.add(method);
				bestParamNumDiff = paramNumDiff;
				continue;
			}
		}

		// bail early
		if (matchedMethods.isEmpty()) {
			return null;
		}

		// now narrow it down to the best method
		// based on argument types
		Method bestMethod = null;
		if (matchedMethods.size()==1 || numParams==0) {
			bestMethod = matchedMethods.iterator().next();

		} else {

			// check the matching methods for
			// matching parameter types
			int mostMatches	= -1;
			for (Method method : matchedMethods) {
				List<Class<?>> parameterTypes = ReflectionUtil.getParameterTypes(method);
				int numMatches = 0;
				for (int i=0; i<parameterTypes.size() && i<numParams; i++) {
					if (isMatchingType(paramNodes.get(i), parameterTypes.get(i))) {
						numMatches++;
					}
				}
				if (numMatches>mostMatches) {
					mostMatches = numMatches;
					bestMethod = method;
				}
			}
		}

		// create return
		MethodAndArgs ret = new MethodAndArgs();
		ret.method = bestMethod;

		// now fill arguments
		int numParameters = bestMethod.getParameterTypes().length;
		for (int i=0; i<numParameters; i++) {
			if (i<numParams) {
				ret.arguments.add(paramNodes.get(i));
			} else {
				ret.arguments.add(NullNode.getInstance());
			}
		}

		// return the method
		return ret;
	}

	/**
	 * Finds the {@link Method} from the supplied {@link Set} that
	 * best matches the rest of the arguments supplied and returns
	 * it as a {@link MethodAndArgs} class.
	 *
	 * @param methods the {@link Method}s
	 * @param paramNames the parameter names
	 * @param paramNodes the parameters for matching types
	 * @return the {@link MethodAndArgs}
	 */
	private MethodAndArgs findBestMethodUsingParamNames(
		Set<Method> methods, Set<String> paramNames, ObjectNode paramNodes) {

		// determine param count
		int maxMatchingParams 				= -1;
		int maxMatchingParamTypes			= -1;
		Method bestMethod 					= null;
		List<JsonRpcParam> bestAnnotations	= null;

		for (Method method : methods) {

			// get parameter types
			List<Class<?>> parameterTypes = ReflectionUtil.getParameterTypes(method);

			// bail early if possible
			if (!allowExtraParams && paramNames.size()>parameterTypes.size()) {
				continue;
			} else if (!allowLessParams && paramNames.size()<parameterTypes.size()) {
				continue;
			}

			// list of params
			List<JsonRpcParam> annotations = new ArrayList<JsonRpcParam>();

			// now try the non-deprecated parameters
			List<List<JsonRpcParam>> methodAnnotations = ReflectionUtil
				.getParameterAnnotations(method, JsonRpcParam.class);
			if (!methodAnnotations.isEmpty()) {
				for (List<JsonRpcParam> annots : methodAnnotations) {
					if (annots.size()>0) {
						annotations.add(annots.get(0));
					} else {
						annots.add(null);
					}
				}
			}

			// count the matching params for this method
			int numMatchingParamTypes = 0;
			int numMatchingParams = 0;
			for (int i=0; i<annotations.size(); i++) {

				// skip parameters that didn't have an annotation
				JsonRpcParam annotation	= annotations.get(i);
				if (annotation==null) {
					continue;
				}

				// check for a match
				String paramName			= annotation.value();
				boolean hasParamName 		= paramNames.contains(paramName);

				if (hasParamName && isMatchingType(paramNodes.get(paramName), parameterTypes.get(i))) {
					numMatchingParamTypes++;
					numMatchingParams++;

				} else if (hasParamName) {
					numMatchingParams++;

				}
			}

			// check for exact param matches
			// bail early if possible
			if (!allowExtraParams && numMatchingParams>parameterTypes.size()) {
				continue;
			} else if (!allowLessParams && numMatchingParams<parameterTypes.size()) {
				continue;
			}

			// better match
			if (numMatchingParams>maxMatchingParams
				|| (numMatchingParams==maxMatchingParams && numMatchingParamTypes>maxMatchingParamTypes)) {
				bestMethod 				= method;
				maxMatchingParams 		= numMatchingParams;
				maxMatchingParamTypes 	= numMatchingParamTypes;
				bestAnnotations 		= annotations;
			}
		}

		// bail early
		if (bestMethod==null) {
			return null;
		}

		// create return
		MethodAndArgs ret = new MethodAndArgs();
		ret.method = bestMethod;

		// now fill arguments
		int numParameters = bestMethod.getParameterTypes().length;
		for (int i=0; i<numParameters; i++) {
			JsonRpcParam param = bestAnnotations.get(i);
			if (param!=null && paramNames.contains(param.value())) {
				ret.arguments.add(paramNodes.get(param.value()));
			} else {
				ret.arguments.add(NullNode.getInstance());
			}
		}

		// return the method
		return ret;
	}

	/**
	 * Determines whether or not the given {@link JsonNode} matches
	 * the given type.  This method is limitted to a few java types
	 * only and shouldn't be used to determine with great accuracy
	 * whether or not the types match.
	 *
	 * @param node the {@link JsonNode}
	 * @param type the {@link Class}
	 * @return true if the types match, false otherwise
	 */
	private boolean isMatchingType(JsonNode node, Class<?> type) {

		if (node.isNull()) {
			return true;

		} else if (node.isTextual()) {
			return String.class.isAssignableFrom(type);

		} else if (node.isNumber()) {
			return Number.class.isAssignableFrom(type)
				|| short.class.isAssignableFrom(type)
				|| int.class.isAssignableFrom(type)
				|| long.class.isAssignableFrom(type)
				|| float.class.isAssignableFrom(type)
				|| double.class.isAssignableFrom(type);

		} else if (node.isArray() && type.isArray()) {
			return (node.size()>0)
				? isMatchingType(node.get(0), type.getComponentType())
				: false;

		} else if (node.isArray()) {
			return type.isArray() || Collection.class.isAssignableFrom(type);

		} else if (node.isBinary()) {
			return byte[].class.isAssignableFrom(type)
				|| Byte[].class.isAssignableFrom(type)
				|| char[].class.isAssignableFrom(type)
				|| Character[].class.isAssignableFrom(type);

		} else if (node.isBoolean()) {
			return boolean.class.isAssignableFrom(type)
				|| Boolean.class.isAssignableFrom(type);

		} else if (node.isObject() || node.isPojo()) {
			return !type.isPrimitive()
				&& !String.class.isAssignableFrom(type)
				&& !Number.class.isAssignableFrom(type)
				&& !Boolean.class.isAssignableFrom(type);
		}

		// not sure if it's a matching type
		return false;
	}

	/**
	 * Simple inner class for the {@code findXXX} methods.
	 */
	private static class MethodAndArgs {
		private Method method = null;
		private List<JsonNode> arguments = new ArrayList<JsonNode>();
	}

	/**
	 * Sets whether or not the server should be backwards
	 * compatible to JSON-RPC 1.0.  This only includes the
	 * omission of the jsonrpc property on the request object,
	 * not the class hinting.
	 *
	 * @param backwardsComaptible the backwardsComaptible to set
	 */
	public void setBackwardsComaptible(boolean backwardsComaptible) {
		this.backwardsComaptible = backwardsComaptible;
	}

	/**
	 * Sets whether or not the server should re-throw exceptions.
	 *
	 * @param rethrowExceptions true or false
	 */
	public void setRethrowExceptions(boolean rethrowExceptions) {
		this.rethrowExceptions = rethrowExceptions;
	}

	/**
	 * Sets whether or not the server should allow superfluous
	 * parameters to method calls.
	 *
	 * @param allowExtraParams true or false
	 */
	public void setAllowExtraParams(boolean allowExtraParams) {
		this.allowExtraParams = allowExtraParams;
	}

	/**
	 * Sets whether or not the server should allow less parameters
	 * than required to method calls (passing null for missing params).
	 *
	 * @param allowLessParams the allowLessParams to set
	 */
	public void setAllowLessParams(boolean allowLessParams) {
		this.allowLessParams = allowLessParams;
	}

	/**
	 * Sets the {@link ErrorResolver} used for resolving errors.
	 * Multiple {@link ErrorResolver}s can be used at once by
	 * using the {@link MultipleErrorResolver}.
	 *
	 * @param errorResolver the errorResolver to set
	 * @see MultipleErrorResolver
	 */
	public void setErrorResolver(ErrorResolver errorResolver) {
		this.errorResolver = errorResolver;
	}

}
