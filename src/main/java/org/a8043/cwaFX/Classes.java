package org.a8043.cwaFX;

import lombok.AccessLevel;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Classes {
    @Getter(AccessLevel.PACKAGE)
    private final Map<Class<?>, List<Annotation>> classMap = new HashMap<>();

    public <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> type) {
        if (!classMap.containsKey(clazz)) {
            return null;
        }
        return classMap.get(clazz).stream()
            .filter(a -> a.annotationType().equals(type))
            .findFirst()
            .map(type::cast)
            .orElse(null);
    }

    public List<Method> getMethods(Class<?> clazz, Class<? extends Annotation> type) {
        return Stream.of(clazz.getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(type))
            .toList();
    }

    public List<Field> getFields(Class<?> clazz, Class<? extends Annotation> type) {
        return Stream.of(clazz.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(type))
            .toList();
    }
}
