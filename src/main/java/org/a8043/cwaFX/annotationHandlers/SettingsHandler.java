package org.a8043.cwaFX.annotationHandlers;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.google.auto.service.AutoService;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.annotations.Settings;
import org.a8043.cwaFX.events.DestroyEvent;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;

import java.io.File;

@AutoService(AnnotationHandler.class)
public class SettingsHandler implements AnnotationHandler<Settings> {
    @Override
    public void onEvent(Class<?> clazz, Settings annotation, Event event, AppContext context) {
        File file = new File(context.getCwaFX().getConfig().getSettingsBaseDir(), annotation.value());
        if (event instanceof InitEvent e && e.getSequence() == 1) {
            Object bean = context.getBean(clazz, "");
            if (file.exists()) {
                JSONObject json = new JSONObject(FileUtil.readUtf8String(file));
                BeanUtil.fillBeanWithMap(json, bean, true);
            }
        } else if (event instanceof DestroyEvent) {
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
