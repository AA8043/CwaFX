package org.a8043.cwaFX.annotationHandlers;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.JsonFileStore;
import org.a8043.cwaFX.annotations.Settings;
import org.a8043.cwaFX.annotations.settings.Deserializer;
import org.a8043.cwaFX.annotations.settings.Serializer;
import org.a8043.cwaFX.events.DestroyEvent;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AutoService(AnnotationHandler.class)
public class SettingsHandler implements AnnotationHandler<Settings> {
    @Override
    public void onEvent(Class<?> clazz, Settings annotation, Event event, AppContext context) {
        File settingsBaseDir = context.getCwaFX().getConfig().getSettingsBaseDir();
        File file = new File(settingsBaseDir, annotation.value());
        if (event instanceof InitEvent e && e.getSequence() == 1) {
            Object bean = context.getBean(clazz, "");
            if (file.exists()) {
                JSONObject json = JsonFileStore.read(file, context.getCwaFX(), clazz);
                applySettings(json, bean, file, clazz, context);
            }
        } else if (event instanceof DestroyEvent) {
            Object bean = context.getBean(clazz, "");
            JSONObject json = serializeSettings(bean, clazz, context);
            JsonFileStore.write(file, json.toString());
        }
    }

    private static JSONObject serializeSettings(Object bean, Class<?> clazz, AppContext context) {
        List<Method> serializers = context.getClasses().getMethods(clazz, Serializer.class);

        if (serializers.size() > 1) {
            IllegalStateException e = new IllegalStateException("Multiple @Serializer methods found in " + clazz);
            log.error(e.getMessage(), e);
            throw e;
        }

        if (!serializers.isEmpty()) {
            Method serializer = serializers.getFirst();

            if (serializer.getParameterCount() != 0) {
                IllegalStateException e = new IllegalStateException(
                    "@Serializer method " + serializer + " must accept no parameters");
                log.error(e.getMessage(), e);
                throw e;
            }

            if (serializer.getReturnType() != JSONObject.class) {
                IllegalStateException e = new IllegalStateException(
                    "@Serializer method " + serializer + " must return JSONObject");
                log.error(e.getMessage(), e);
                throw e;
            }

            try {
                serializer.setAccessible(true);
                return (JSONObject) serializer.invoke(bean);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to invoke @Serializer method " + serializer, exception);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to serialize settings in " + clazz, e);
            }
        }

        return new JSONObject(bean);
    }

    private static void applySettings(JSONObject json, Object bean, File file, Class<?> clazz, AppContext context) {
        Map<Field, Object> original = new LinkedHashMap<>();
        for (Class<?> type = bean.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    original.put(field, field.get(bean));
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Unable to snapshot settings field " + field, exception);
                }
            }
        }

        List<Method> deserializers = context.getClasses().getMethods(clazz, Deserializer.class);

        if (deserializers.size() > 1) {
            IllegalStateException e = new IllegalStateException("Multiple @Deserializer methods found in " + clazz);
            log.error(e.getMessage(), e);
            throw e;
        }

        Method deserializer = deserializers.isEmpty() ? null : deserializers.getFirst();
        if (deserializer != null) {
            if (deserializer.getParameterCount() != 1 || deserializer.getParameterTypes()[0] != JSONObject.class) {
                IllegalStateException e = new IllegalStateException(
                    "@Deserializer method " + deserializer + " must accept a single JSONObject parameter");
                log.error(e.getMessage(), e);
                throw e;
            }

            if (deserializer.getReturnType() != void.class) {
                IllegalStateException e = new IllegalStateException(
                    "@Deserializer method " + deserializer + " must return void");
                log.error(e.getMessage(), e);
                throw e;
            }

            deserializer.setAccessible(true);
        }

        try {
            if (deserializer != null) {
                deserializer.invoke(bean, json);
            } else {
                BeanUtil.fillBeanWithMap(json, bean, true);
            }
        } catch (InvocationTargetException | RuntimeException exception) {
            Throwable failure = exception instanceof InvocationTargetException invocation
                ? invocation.getTargetException()
                : exception;
            if (failure instanceof Error error) {
                throw error;
            }
            original.forEach((field, value) -> {
                try {
                    field.set(bean, value);
                } catch (IllegalAccessException restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            });
            JsonFileStore.reportCorruption(file, context.getCwaFX(), clazz, failure);
            log.warn("Ignoring invalid settings in {}", file, failure);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to invoke @Deserializer method " + deserializer, exception);
        }
    }

    @Override
    public Class<Settings> getType() {
        return Settings.class;
    }
}
