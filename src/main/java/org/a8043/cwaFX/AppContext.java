package org.a8043.cwaFX;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.events.NewBeanEvent;
import org.a8043.cwaFX.window.Window;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Getter
public class AppContext {
    private final CwaFX cwaFX;
    private final String[] args;

    private final Classes classes = new Classes();
    private final Map<BeanKey, Object> beans = new ConcurrentHashMap<>();
    private final Map<String, Window> windows = new HashMap<>();
    private final Map<FieldAccessor, PendingDependency> injectionFailures = new LinkedHashMap<>();
    private final Set<Object> initializedBeans = Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile boolean initializationComplete;

    public <T> T findBean(Class<T> clazz, String name) {
        if (name.isEmpty()) {
            List<Object> beansOfType = beans.entrySet().stream()
                .filter(entry -> entry.getKey().getClazz().equals(clazz))
                .map(Map.Entry::getValue)
                .toList();
            return beansOfType.size() == 1 ? (T) beansOfType.getFirst() : null;
        }

        Bean annotation = classes.getAnnotation(clazz, Bean.class);
        String keyName = annotation != null && annotation.single()
            ? clazz.getSimpleName()
            : name;
        Object bean = beans.get(new BeanKey(clazz, keyName));
        if (bean != null) {
            return (T) bean;
        }
        return (T) beans.entrySet().stream()
            .filter(entry -> entry.getKey().getClazz().equals(clazz))
            .filter(entry -> entry.getKey().getName().equalsIgnoreCase(keyName))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    public <T> T findBeanExact(Class<T> clazz, String name) {
        if (name == null || name.isEmpty()) return null;
        return (T) beans.entrySet().stream()
            .filter(entry -> entry.getKey().getClazz().equals(clazz))
            .filter(entry -> entry.getKey().getName().equalsIgnoreCase(new BeanKey(clazz, name).getName()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    public <T> T getBean(Class<T> clazz, String name, Object... args) {
        if (name.isEmpty()) {
            List<Object> beansOfType = beans.entrySet().stream()
                .filter(entry -> entry.getKey().getClazz().equals(clazz))
                .map(Map.Entry::getValue)
                .toList();
            if (beansOfType.size() == 1) {
                return (T) beansOfType.getFirst();
            }
        }

        Bean annotation = classes.getAnnotation(clazz, Bean.class);
        String keyName;
        if (annotation == null && name.isEmpty()) {
            keyName = clazz.getSimpleName();
        } else if (annotation != null && annotation.single()) {
            keyName = clazz.getSimpleName();
        } else {
            keyName = name;
        }

        BeanKey key = new BeanKey(clazz, keyName);
        Object existing = beans.get(key);
        if (existing != null) {
            return (T) existing;
        }

        Object created = instantiate(clazz, args, key);

        Object bean = beans.putIfAbsent(key, created);
        if (bean != null) {
            return (T) bean;
        }

        cwaFX.notifyEvent(new NewBeanEvent(key, created));
        return (T) created;
    }

    /** Creates a controller instance owned by a page navigation entry. */
    public <T> T createPageBean(Class<T> clazz) {
        BeanKey key = new BeanKey(clazz, "page-" + UUID.randomUUID());
        return (T) instantiate(clazz, new Object[0], key);
    }

    public List<Object> getBeans(Class<?> clazz) {
        return beans.values().stream()
            .filter(obj -> obj.getClass() == clazz)
            .collect(Collectors.toList());
    }

    /**
     * Returns beans registered under the supplied type, including interface and
     * superclass registrations whose concrete instance has a different class.
     */
    public List<Object> getBeansByKeyClass(Class<?> clazz) {
        return beans.entrySet().stream()
            .filter(entry -> entry.getKey().getClazz().equals(clazz))
            .map(Map.Entry::getValue)
            .toList();
    }

    public void addBean(BeanKey key, Object bean) {
        Objects.requireNonNull(key, "Bean key must not be null.");
        Objects.requireNonNull(bean, "Bean must not be null.");
        beans.put(key, bean);
        cwaFX.notifyEvent(new NewBeanEvent(key, bean));
    }

    void completeInitialization() {
        initializationComplete = true;
    }

    public synchronized boolean markBeanInitialized(Object bean) {
        return initializedBeans.add(bean);
    }

    public synchronized void recordInjectionFailure(FieldAccessor accessor, String name, boolean explicitName,
                                                    boolean typeFallback) {
        injectionFailures.put(accessor, new PendingDependency(name, explicitName, typeFallback));
    }

    public synchronized void clearInjectionFailure(FieldAccessor accessor) {
        injectionFailures.remove(accessor);
    }

    public synchronized Map<FieldAccessor, PendingDependency> pendingInjections() {
        return new LinkedHashMap<>(injectionFailures);
    }

    synchronized void assertNoPendingInjections() {
        if (injectionFailures.isEmpty()) {
            return;
        }
        String details = injectionFailures.entrySet().stream()
            .map(entry -> entry.getKey().getObject().getClass().getName() + "."
                + entry.getKey().getField().getName() + " -> "
                + entry.getKey().getField().getType().getTypeName() + " named '"
                + entry.getValue().name() + "'")
            .sorted()
            .collect(Collectors.joining(", "));
        throw new IllegalStateException("Unresolved required @Autowired dependencies: " + details);
    }

    private Object instantiate(Class<?> clazz, Object[] suppliedArgs, BeanKey key) {
        Object[] args = suppliedArgs == null ? new Object[0] : suppliedArgs;
        List<Constructor<?>> matches = Arrays.stream(clazz.getConstructors())
            .filter(constructor -> constructor.getParameterCount() == args.length)
            .filter(constructor -> compatible(constructor.getParameterTypes(), args))
            .sorted(Comparator.comparingInt(constructor -> matchScore(constructor.getParameterTypes(), args)))
            .toList();
        if (matches.isEmpty()) {
            throw new IllegalStateException("No compatible public constructor for " + key
                + " with argument types " + argumentTypes(args));
        }
        int bestScore = matchScore(matches.getFirst().getParameterTypes(), args);
        List<Constructor<?>> best = matches.stream()
            .filter(constructor -> matchScore(constructor.getParameterTypes(), args) == bestScore)
            .toList();
        if (best.size() != 1) {
            throw new IllegalStateException("Ambiguous public constructors for " + key
                + " with argument types " + argumentTypes(args) + ": " + best);
        }
        Constructor<?> constructor = best.getFirst();
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("Constructor " + constructor + " failed while creating " + key, cause);
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to invoke constructor " + constructor + " for " + key, exception);
        }
    }

    private static boolean compatible(Class<?>[] parameterTypes, Object[] args) {
        for (int index = 0; index < args.length; index++) {
            Class<?> parameter = parameterTypes[index];
            Object argument = args[index];
            if (argument == null) {
                if (parameter.isPrimitive()) return false;
            } else if (!wrap(parameter).isAssignableFrom(argument.getClass())) {
                return false;
            }
        }
        return true;
    }

    private static int matchScore(Class<?>[] parameterTypes, Object[] args) {
        int score = 0;
        for (int index = 0; index < args.length; index++) {
            Class<?> parameter = parameterTypes[index];
            Object argument = args[index];
            if (argument == null) {
                score += 16;
            } else if (parameter.equals(argument.getClass())) {
                score += 0;
            } else if (wrap(parameter).equals(argument.getClass())) {
                score += 1;
            } else {
                score += 4 + inheritanceDistance(argument.getClass(), wrap(parameter));
            }
        }
        return score;
    }

    private static int inheritanceDistance(Class<?> concrete, Class<?> target) {
        if (concrete.equals(target)) return 0;
        if (target.isInterface()) return 1;
        int distance = 0;
        for (Class<?> type = concrete; type != null && !type.equals(target); type = type.getSuperclass()) {
            distance++;
        }
        return distance;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == boolean.class) return Boolean.class;
        return Void.class;
    }

    private static List<String> argumentTypes(Object[] args) {
        return Arrays.stream(args).map(arg -> arg == null ? "null" : arg.getClass().getTypeName()).toList();
    }

    public record PendingDependency(String name, boolean explicitName, boolean typeFallback) {
    }
}
