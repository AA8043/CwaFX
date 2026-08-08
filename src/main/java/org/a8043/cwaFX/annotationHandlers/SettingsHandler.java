package org.a8043.cwaFX.annotationHandlers;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.annotations.Settings;
import org.a8043.cwaFX.events.DestroyEvent;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;

import java.io.File;

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
                JSONObject json = new JSONObject(FileUtil.readUtf8String(file));
                BeanUtil.fillBeanWithMap(json, bean, true);
            }
        } else if (event instanceof DestroyEvent) {
            if (!settingsBaseDir.exists() && !settingsBaseDir.mkdirs()) {
                log.error("Error creating settings directory: {}", settingsBaseDir);
                return;
            }
            Object bean = context.getBean(clazz, "");
            JSONObject json = new JSONObject(bean);
            FileUtil.writeUtf8String(json.toString(), file);
        }
    }

    @Override
    public Class<Settings> getType() {
        return Settings.class;
    }
}
