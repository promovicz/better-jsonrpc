package better.jsonrpc.util;

import better.jsonrpc.annotations.JsonRpcInterface;
import better.jsonrpc.annotations.JsonRpcMethod;
import better.jsonrpc.annotations.JsonRpcNotification;
import better.jsonrpc.annotations.JsonRpcParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for reflection.
 */
public abstract class ReflectionUtil {

	private static Map<String, Set<Method>> methodCache
		= new HashMap<>();

	private static Map<Method, List<Class<?>>> parameterTypeCache
		= new HashMap<>();

	private static Map<Class, List<Annotation>> classAnnotationCache
			= new HashMap<>();

	private static Map<Method, List<Annotation>> methodAnnotationCache
		= new HashMap<>();

	private static Map<Method, List<List<Annotation>>> methodParamAnnotationCache
		= new HashMap<>();

	/**
	 * Finds methods with the given name on the given class.
	 * @param clazzes the classes
	 * @param name the method name
	 * @return the methods
	 */
	public static Set<Method> findMethods(Class<?>[] clazzes, String name) {
		StringBuilder sb = new StringBuilder();
		for (Class<?> clazz : clazzes) {
			sb.append(clazz.getName()).append("::");
		}
		String cacheKey = sb.append(name).toString();
		if (methodCache.containsKey(cacheKey)) {
			return methodCache.get(cacheKey);
		}
		Set<Method> methods = new HashSet<Method>();
		for (Class<?> clazz : clazzes) {
            String clazzPrefix = "";
            JsonRpcInterface clazzAnnotation = clazz.getAnnotation(JsonRpcInterface.class);
            if(clazzAnnotation != null) {
                if(!clazzAnnotation.prefix().isEmpty()) {
                    clazzPrefix = clazzAnnotation.prefix();
                }
            }
			for (Method method : clazz.getMethods()) {
                String methodName = clazzPrefix + method.getName();
                JsonRpcMethod methodAnnotation = method.getAnnotation(JsonRpcMethod.class);
                if(methodAnnotation != null) {
                    if(!methodAnnotation.name().isEmpty()) {
                        methodName = clazzPrefix + methodAnnotation.name();
                    }
                }
				if (methodName.equals(name)) {
					methods.add(method);
				}
			}
		}
		methods = Collections.unmodifiableSet(methods);
		methodCache.put(cacheKey, methods);
		return methods;
	}

	/**
	 * Returns the parameter types for the given {@link Method}.
	 * @param method the {@link Method}
	 * @return the parameter types
	 */
	public static List<Class<?>> getParameterTypes(Method method) {
		if (parameterTypeCache.containsKey(method)) {
			return parameterTypeCache.get(method);
		}
		List<Class<?>> types = new ArrayList<>();
		types.addAll(Arrays.asList(method.getParameterTypes()));
		types = Collections.unmodifiableList(types);
		parameterTypeCache.put(method, types);
		return types;
	}

	/**
	 * Returns all of the {@link Annotation}s defined on
	 * the given {@link Class}.
	 * @param clazz the {@link Class}
	 * @return the {@link Annotation}s
	 */
	public static List<Annotation> getAnnotations(Class<?> clazz) {
		if (classAnnotationCache.containsKey(clazz)) {
			return classAnnotationCache.get(clazz);
		}
		List<Annotation> annotations = new ArrayList<>();
		annotations.addAll(Arrays.asList(clazz.getAnnotations()));
		annotations = Collections.unmodifiableList(annotations);
		classAnnotationCache.put(clazz, annotations);
		return annotations;
	}

	/**
	 * Returns all of the {@link Annotation}s defined on
	 * the given {@link Method}.
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	public static List<Annotation> getAnnotations(Method method) {
		if (methodAnnotationCache.containsKey(method)) {
			return methodAnnotationCache.get(method);
		}
		List<Annotation> annotations = new ArrayList<>();
		annotations.addAll(Arrays.asList(method.getAnnotations()));
		annotations = Collections.unmodifiableList(annotations);
		methodAnnotationCache.put(method, annotations);
		return annotations;
	}

	/**
	 * Returns {@link Annotation}s of the given type defined
	 * on the given {@link Class}.
	 * @param <T> the {@link Annotation} type
	 * @param clazz the {@link Class}
	 * @param type the type
	 * @return the {@link Annotation}s
	 */
	public static <T extends Annotation>
	List<T> getAnnotations(Class<?> clazz, Class<T> type) {
		List<T> ret = new ArrayList<T>();
		for (Annotation a : getAnnotations(clazz)) {
			if (type.isInstance(a)) {
				ret.add(type.cast(a));
			}
		}
		return ret;
	}

	/**
	 * Returns the first {@link Annotation} of the given type
	 * defined on the given {@link Class}.
	 * @param <T> the type
	 * @param clazz the class
	 * @param type the type of annotation
	 * @return the annotation or null
	 */
	public static <T extends Annotation>
	T getAnnotation(Class clazz, Class<T> type) {
		for (Annotation a : getAnnotations(clazz)) {
			if (type.isInstance(a)) {
				return type.cast(a);
			}
		}
		return null;
	}

	/**
	 * Returns {@link Annotation}s of the given type defined
	 * on the given {@link Method}.
	 * @param <T> the {@link Annotation} type
	 * @param method the {@link Method}
	 * @param type the type
	 * @return the {@link Annotation}s
	 */
	public static <T extends Annotation>
	List<T> getAnnotations(Method method, Class<T> type) {
		List<T> ret = new ArrayList<T>();
		for (Annotation a : getAnnotations(method)) {
			if (type.isInstance(a)) {
				ret.add(type.cast(a));
			}
		}
		return ret;
	}

	/**
	 * Returns the first {@link Annotation} of the given type
	 * defined on the given {@link Method}.
	 * @param <T> the type
	 * @param method the method
	 * @param type the type of annotation
	 * @return the annotation or null
	 */
	public static <T extends Annotation>
	T getAnnotation(Method method, Class<T> type) {
		for (Annotation a : getAnnotations(method)) {
			if (type.isInstance(a)) {
				return type.cast(a);
			}
		}
		return null;
	}

	/**
	 * Returns the parameter {@link Annotation}s for the
	 * given {@link Method}.
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	public static List<List<Annotation>> getParameterAnnotations(Method method) {
		if (methodParamAnnotationCache.containsKey(method)) {
			return methodParamAnnotationCache.get(method);
		}
		List<List<Annotation>> annotations = new ArrayList<>();
		for (Annotation[] paramAnnotations : method.getParameterAnnotations()) {
			List<Annotation> listAnnotations = new ArrayList<>();
			listAnnotations.addAll(Arrays.asList(paramAnnotations));
			annotations.add(listAnnotations);
		}
		annotations = Collections.unmodifiableList(annotations);
		methodParamAnnotationCache.put(method, annotations);
		return annotations;
	}

	/**
	 * Returns the parameter {@link Annotation}s of the
	 * given type for the given {@link Method}.
	 * @param <T> the {@link Annotation} type
	 * @param type the type
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	public static <T extends Annotation>
		List<List<T>> getParameterAnnotations(Method method, Class<T> type) {
		List<List<T>> annotations = new ArrayList<>();
		for (List<Annotation> paramAnnotations : getParameterAnnotations(method)) {
			List<T> listAnnotations = new ArrayList<>();
			for (Annotation a : paramAnnotations) {
				if (type.isInstance(a)) {
					listAnnotations.add(type.cast(a));
				}
			}
			annotations.add(listAnnotations);
		}
		return annotations;
	}

	/**
	 * Parses the given arguments for the given method optionally
	 * turning them into named parameters.
	 * @param method the method
	 * @param arguments the arguments
	 * @param useNamedParams whether or not to used named params
	 * @return the parsed arguments
	 */
	public static Object parseArguments(Method method, Object[] arguments, boolean useNamedParams) {
		if (useNamedParams) {
			Map<String, Object> namedParams = new HashMap<>();
			Annotation[][] paramAnnotations = method.getParameterAnnotations();
			for (int i=0; i<paramAnnotations.length; i++) {
				Annotation[] ann = paramAnnotations[i];
				boolean jsonRpcParamAnnotPresent = false;
				for (Annotation an : ann) {
					if (JsonRpcParam.class.isInstance(an)) {
						JsonRpcParam jAnn = (JsonRpcParam) an;
						namedParams.put(jAnn.value(), arguments[i]);
						jsonRpcParamAnnotPresent = true;
						break;
					}
				}
				if (!jsonRpcParamAnnotPresent) {
					throw new RuntimeException(
						"useNamedParams is enabled and a JsonRpcParam annotation "
						+"was not found at parameter index "+i+" on method "
						+method.getName());
				}
			}
			return namedParams;
		} else {
			return arguments;
		}
	}

    public static boolean isNotification(Method method) {
        return getAnnotation(method, JsonRpcNotification.class) != null;
    }

    public static class ReflectedInterface {
        private final Class<?> mClass;
        private final HashMap<String, ReflectedMethod> mMethods;
        public ReflectedInterface(Class<?> clazz) {
            mClass = clazz;
            mMethods = new HashMap<String, ReflectedMethod>();
        }
    }

    public static class ReflectedMethod {
        Method mMethod;
        String mName;
        boolean mIsNotification = false;
        public ReflectedMethod(Method method) {
            mMethod = method;
            mName = method.getName();
            mIsNotification = isNotification(method);
        }
    }

}
