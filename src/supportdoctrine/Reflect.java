package supportdoctrine;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal reflection helper. Starsector refuses to load mod classes that reference
 * java.lang.reflect directly, so (like LunaLib / Intel Renewed) we only touch Field, Method and
 * Constructor through MethodHandles and keep them typed as Object.
 */
final class Reflect {

    private static final MethodHandle GET_DECLARED_FIELDS;
    private static final MethodHandle FIELD_GET_TYPE;
    private static final MethodHandle FIELD_SET_ACCESSIBLE;
    private static final MethodHandle FIELD_GET;

    private static final MethodHandle GET_METHODS;
    private static final MethodHandle METHOD_GET_NAME;
    private static final MethodHandle METHOD_GET_PARAM_COUNT;
    private static final MethodHandle METHOD_INVOKE;

    private static final MethodHandle GET_CONSTRUCTORS;
    private static final MethodHandle CTOR_GET_PARAM_TYPES;
    private static final MethodHandle CTOR_NEW_INSTANCE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            ClassLoader boot = Class.class.getClassLoader();
            Class<?> fieldClass = Class.forName("java.lang.reflect.Field", false, boot);
            Class<?> fieldArray = Class.forName("[Ljava.lang.reflect.Field;", false, boot);
            Class<?> methodClass = Class.forName("java.lang.reflect.Method", false, boot);
            Class<?> methodArray = Class.forName("[Ljava.lang.reflect.Method;", false, boot);
            Class<?> ctorClass = Class.forName("java.lang.reflect.Constructor", false, boot);
            Class<?> ctorArray = Class.forName("[Ljava.lang.reflect.Constructor;", false, boot);

            GET_DECLARED_FIELDS = lookup.findVirtual(Class.class, "getDeclaredFields", MethodType.methodType(fieldArray));
            FIELD_GET_TYPE = lookup.findVirtual(fieldClass, "getType", MethodType.methodType(Class.class));
            FIELD_SET_ACCESSIBLE = lookup.findVirtual(fieldClass, "setAccessible", MethodType.methodType(void.class, boolean.class));
            FIELD_GET = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));

            GET_METHODS = lookup.findVirtual(Class.class, "getMethods", MethodType.methodType(methodArray));
            METHOD_GET_NAME = lookup.findVirtual(methodClass, "getName", MethodType.methodType(String.class));
            METHOD_GET_PARAM_COUNT = lookup.findVirtual(methodClass, "getParameterCount", MethodType.methodType(int.class));
            METHOD_INVOKE = lookup.findVirtual(methodClass, "invoke",
                    MethodType.methodType(Object.class, Object.class, Object[].class)).asFixedArity();

            GET_CONSTRUCTORS = lookup.findVirtual(Class.class, "getConstructors", MethodType.methodType(ctorArray));
            CTOR_GET_PARAM_TYPES = lookup.findVirtual(ctorClass, "getParameterTypes", MethodType.methodType(Class[].class));
            CTOR_NEW_INSTANCE = lookup.findVirtual(ctorClass, "newInstance",
                    MethodType.methodType(Object.class, Object[].class)).asFixedArity();
        } catch (Throwable t) {
            throw new IllegalStateException("Support Doctrine: reflection setup failed", t);
        }
    }

    private static final Map<String, Object> METHOD_CACHE = new HashMap<>();

    private Reflect() {
    }

    /** Value of the first declared field (on the class or a superclass) whose type has the given name. */
    static Object getFieldOfType(Object target, String typeName) throws Throwable {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Object field : (Object[]) GET_DECLARED_FIELDS.invoke(c)) {
                Class<?> type = (Class<?>) FIELD_GET_TYPE.invoke(field);
                if (type.getName().equals(typeName)) {
                    FIELD_SET_ACCESSIBLE.invoke(field, true);
                    return FIELD_GET.invoke(field, target);
                }
            }
        }
        return null;
    }

    static boolean hasMethod(Object target, String name, int paramCount) throws Throwable {
        return findMethod(target.getClass(), name, paramCount) != null;
    }

    /** Calls a public method by name and parameter count. */
    static Object call(Object target, String name, Object... args) throws Throwable {
        Object method = findMethod(target.getClass(), name, args.length);
        if (method == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "." + name + "/" + args.length);
        }
        return METHOD_INVOKE.invoke(method, target, args);
    }

    private static Object findMethod(Class<?> cls, String name, int paramCount) throws Throwable {
        String key = cls.getName() + "#" + name + "/" + paramCount;
        if (METHOD_CACHE.containsKey(key)) return METHOD_CACHE.get(key);
        Object found = null;
        for (Object method : (Object[]) GET_METHODS.invoke(cls)) {
            if (name.equals(METHOD_GET_NAME.invoke(method))
                    && (int) METHOD_GET_PARAM_COUNT.invoke(method) == paramCount) {
                found = method;
                break;
            }
        }
        METHOD_CACHE.put(key, found);
        return found;
    }

    /** Finds a public constructor whose parameter type names match exactly (primitive names like "boolean"). */
    static Object findConstructor(Class<?> cls, String... paramTypeNames) throws Throwable {
        for (Object ctor : (Object[]) GET_CONSTRUCTORS.invoke(cls)) {
            Class<?>[] params = (Class<?>[]) CTOR_GET_PARAM_TYPES.invoke(ctor);
            if (params.length != paramTypeNames.length) continue;
            boolean match = true;
            for (int i = 0; i < params.length; i++) {
                if (!params[i].getName().equals(paramTypeNames[i])) {
                    match = false;
                    break;
                }
            }
            if (match) return ctor;
        }
        return null;
    }

    static Object newInstance(Object ctor, Object... args) throws Throwable {
        return CTOR_NEW_INSTANCE.invoke(ctor, args);
    }
}
