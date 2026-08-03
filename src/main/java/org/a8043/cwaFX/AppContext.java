package org.a8043.cwaFX;

import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.a8043.cwaFX.annotations.bean.Bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Getter
public class AppContext {
    private final CwaFX cwaFX;
    private final String[] args;

    private final Classes classes = new Classes();
    private final Map<BeanKey, Object> beans = new HashMap<>();
    private final Map<String, Stage> stages = new HashMap<>();

    public Object getBean(Class<?> clazz, String name) {
        BeanKey key = new BeanKey(clazz, classes.getAnnotation(clazz, Bean.class).single() ?
            clazz.getSimpleName() : name);
        return beans.get(key);
    }

    public List<Object> getBeans(Class<?> clazz) {
        return beans.values().stream()
            .filter(obj -> obj.getClass() == clazz)
            .collect(java.util.stream.Collectors.toList());
    }
}
