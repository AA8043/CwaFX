package org.a8043.cwaFX;

import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.annotations.bean.OnFXThread;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Util {
    public static List<Method> getMethods(Class<?> clazz, Class<? extends Annotation> annotation) {
        return Stream.of(clazz.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(annotation))
            .collect(Collectors.toList());
    }

    public static void invokeMethod(Method method, Object object, Object... args) {
        method.setAccessible(true);
        Runnable invocation = () -> invoke(method, object, args);
        if (method.isAnnotationPresent(OnFXThread.class)
            && !javafx.application.Platform.isFxApplicationThread()) {
            CwaPlatform.runOnFxThread(() -> {
                invocation.run();
                return null;
            });
        } else {
            invocation.run();
        }
    }

    private static void invoke(Method method, Object object, Object[] args) {
        try {
            method.invoke(object, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Error invoking method " + method, cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Error invoking method " + method, exception);
        }
    }
}
