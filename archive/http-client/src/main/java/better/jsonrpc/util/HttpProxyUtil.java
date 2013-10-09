package better.jsonrpc.util;

import better.jsonrpc.client.JsonRpcHttpClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class HttpProxyUtil {

	private static final Logger LOGGER = Logger.getLogger(HttpProxyUtil.class.getName());
	

	/**
	 * Creates a {@link Proxy} of the given {@link proxyInterface}
	 * that uses the given {@link JsonRpcHttpClient}.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the {@link JsonRpcHttpClient}
	 * @param extraHeaders extra HTTP headers to be added to each response
	 * @return the proxied interface
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final boolean useNamedParams,
		final JsonRpcHttpClient client,
		final Map<String, String> extraHeaders) {

		// create and return the proxy
		return (T)Proxy.newProxyInstance(
			classLoader,
			new Class<?>[] {proxyInterface},
			new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
					Object arguments = ReflectionUtil.parseArguments(method, args, useNamedParams);
					return client.invoke(
						method.getName(), arguments, method.getGenericReturnType(), extraHeaders);
				}
			});
	}

	/**
	 * Creates a {@link Proxy} of the given {@link proxyInterface}
	 * that uses the given {@link JsonRpcHttpClient}.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the {@link JsonRpcHttpClient}
	 * @return the proxied interface
	 */
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final JsonRpcHttpClient client) {
		return createClientProxy(classLoader, proxyInterface, false, client, new HashMap<String, String>());
	}
	
}
