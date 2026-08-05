package org.a8043.cwaFX;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.events.NewBeanEvent;
import org.a8043.cwaFX.window.Window;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final Set<Object> initializedBeans = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile boolean initializationComplete;

    public Object getBean(Class<?> clazz, String name) {
        if (name.isEmpty()) {
            List<Object> beansOfType = beans.entrySet().stream()
                .filter(entry -> entry.getKey().getClazz().equals(clazz))
                .map(Map.Entry::getValue)
                .toList();
            if (beansOfType.size() == 1) {
                return beansOfType.getFirst();
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
        return beans.computeIfAbsent(key, k -> {
            try {
                Object object = clazz.getConstructor().newInstance();
                cwaFX.notifyEvent(new NewBeanEvent(new BeanKey(clazz, name), object));
                return object;
            } catch (Exception e) {
                return null;
            }
        });
    }

    public List<Object> getBeans(Class<?> clazz) {
        return beans.values().stream()
            .filter(obj -> obj.getClass() == clazz)
            .collect(Collectors.toList());
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
