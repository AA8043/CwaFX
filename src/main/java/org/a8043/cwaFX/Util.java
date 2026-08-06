package org.a8043.cwaFX;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.annotations.bean.OnFXThread;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
        CountDownLatch latch = new CountDownLatch(1);

        Runnable runnable = () -> {
            try {
                method.invoke(object, (Object[]) args);
            } catch (Exception e) {
                log.error("Error invoking method: {}", method.getName(), e);
            } finally {
                latch.countDown();
            }
        };

        if (method.isAnnotationPresent(OnFXThread.class)) {
            Platform.runLater(runnable);
        } else {
            runnable.run();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
