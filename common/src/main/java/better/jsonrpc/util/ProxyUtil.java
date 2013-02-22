package better.jsonrpc.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.VoiceStatus;

import better.jsonrpc.annotations.JsonRpcNotification;
import better.jsonrpc.client.JsonRpcClient;

/**
 * Utilities for create client proxies.
 */
public abstract class ProxyUtil {

	private static final Logger LOGGER = Logger.getLogger(ProxyUtil.class.getName());

	/**
	 * Creates a composite service using all of the given
	 * services.
	 * 
	 * @param classLoader the {@link ClassLoader}
	 * @param services the service objects
	 * @param allowMultipleInheritance whether or not to allow multiple inheritance
	 * @return the object
	 */
	public static Object createCompositeServiceProxy(
		ClassLoader classLoader, Object[] services, boolean allowMultipleInheritance) {
		return createCompositeServiceProxy(classLoader, services, null, allowMultipleInheritance);
	}

	/**
	 * Creates a composite service using all of the given
	 * services and implementing the given interfaces.
	 * 
	 * @param classLoader the {@link ClassLoader}
	 * @param services the service objects
	 * @param serviceInterfaces the service interfaces
	 * @param allowMultipleInheritance whether or not to allow multiple inheritance
	 * @return the object
	 */
	public static Object createCompositeServiceProxy(
		ClassLoader classLoader, Object[] services,
		Class<?>[] serviceInterfaces, boolean allowMultipleInheritance) {
		
		// get interfaces
		Set<Class<?>> interfaces = new HashSet<Class<?>>();
		if (serviceInterfaces!=null) {
			interfaces.addAll(Arrays.asList(serviceInterfaces));
		} else {
			for (Object o : services) {
				interfaces.addAll(Arrays.asList(o.getClass().getInterfaces()));
			}
		}

		// build the service map
		final Map<Class<?>, Object> serviceMap = new HashMap<Class<?>, Object>();
		for (Class<?> clazz : interfaces) {

			// we will allow for this, but the first
			// object that was registered wins
			if (serviceMap.containsKey(clazz) && allowMultipleInheritance) {
				continue;
			} else if (serviceMap.containsKey(clazz)) {
				throw new IllegalArgumentException(
					"Multiple inheritance not allowed "+clazz.getName());
			}

			// find a service for this interface
			for (Object o : services) {
				if (clazz.isInstance(o)) {
					if (LOGGER.isLoggable(Level.FINE)) {
						LOGGER.fine("Using "+o.getClass().getName()+" for "+clazz.getName());
					}
					serviceMap.put(clazz, o);
					break;
				}
			}

			// make sure we have one
			if (!serviceMap.containsKey(clazz)) {
				throw new IllegalArgumentException(
					"None of the provided services implement "+clazz.getName());
			}
		}

		// now create the proxy
		return Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class<?>[0]),
			new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
				Class<?> clazz = method.getDeclaringClass();
				return method.invoke(serviceMap.get(clazz), args);
			}
		});
	}

	/**
	 * Creates a {@link Proxy} of the given {@link proxyInterface}
	 * that uses the given {@link JsonRpcClient}.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the {@link JsonRpcClient}
	 * @param socket the {@link Socket}
	 * @return the proxied interface
	 */
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final JsonRpcConnection connection) {

		// create and return the proxy
		return createClientProxy(
			classLoader, proxyInterface, false, connection);
	}

	/**
	 * Creates a {@link Proxy} of the given {@link proxyInterface}
	 * that uses the given {@link JsonRpcClient}.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the {@link JsonRpcClient}
	 * @param ips the {@link InputStream}
	 * @param ops the {@link OutputStream}
	 * @return the proxied interface
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final boolean useNamedParams,
		final JsonRpcConnection connection) {

		// create and return the proxy
		return (T)Proxy.newProxyInstance(
			ClassLoader.getSystemClassLoader(),
			new Class<?>[] {proxyInterface},
			new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
					JsonRpcClient client = connection.getClient();
					Object arguments = ReflectionUtil.parseArguments(method, args, useNamedParams);
					if(method.getAnnotation(JsonRpcNotification.class) != null) {
						client.invokeNotification(method.getName(), arguments, connection);
						return null;
					} else {
						return client.invokeMethod(
								method.getName(), arguments,
								method.getGenericReturnType(), connection);
					}
				}
			});
	}

}
