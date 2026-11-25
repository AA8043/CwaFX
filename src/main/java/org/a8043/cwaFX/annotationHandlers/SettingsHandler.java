package org.a8043.cwaFX.annotationHandlers;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.JsonFileStore;
import org.a8043.cwaFX.annotations.Settings;
import org.a8043.cwaFX.events.DestroyEvent;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
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
            JSONObject json = new JSONObject(bean);
            JsonFileStore.write(file, json.toString());
        }
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
        try {
            BeanUtil.fillBeanWithMap(json, bean, true);
        } catch (RuntimeException failure) {
            original.forEach((field, value) -> {
                try {
                    field.set(bean, value);
                } catch (IllegalAccessException restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            });
            JsonFileStore.reportCorruption(file, context.getCwaFX(), clazz, failure);
            log.warn("Ignoring invalid settings in {}", file, failure);
        }
    }

    @Override
    public Class<Settings> getType() {
        return Settings.class;
    }
}
