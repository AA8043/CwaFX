package org.a8043.cwaFX;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.events.NewBeanEvent;
import org.a8043.cwaFX.window.Window;

import java.lang.reflect.Constructor;
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
    private final Map<BeanKey, List<FieldAccessor>> injectionFailures = new HashMap<>();
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
        return (T) beans.get(new BeanKey(clazz, keyName));
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

        final Object created;
        try {
            Class<?>[] argTypes = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class[]::new);

            Constructor<?> ctor = Arrays.stream(clazz.getConstructors())
                .filter(c -> c.getParameterCount() == args.length)
                .filter(c -> {
                    Class<?>[] params = c.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        if (!params[i].isAssignableFrom(argTypes[i])) {
                            return false;
                        }
                    }
                    return true;
                })
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(clazz.getName() + " with args " + Arrays.toString(argTypes)));

            created = ctor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean: " + clazz.getName(), e);
        }

        Object bean = beans.putIfAbsent(key, created);
        if (bean != null) {
            return (T) bean;
        }

        cwaFX.notifyEvent(new NewBeanEvent(key, created));
        return (T) created;
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
        beans.put(key, bean);
        cwaFX.notifyEvent(new NewBeanEvent(key, bean));
    }

    void completeInitialization() {
        initializationComplete = true;
    }

    public synchronized boolean markBeanInitialized(Object bean) {
        return initializedBeans.add(bean);
    }
}
