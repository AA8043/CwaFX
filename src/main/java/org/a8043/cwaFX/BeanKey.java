package org.a8043.cwaFX;

import cn.hutool.core.text.NamingCase;
import lombok.Value;

@Value
public class BeanKey {
    Class<?> clazz;
    String name;

    public BeanKey(Class<?> clazz, String name) {
        this.clazz = clazz;
        this.name = NamingCase.toCamelCase(name);
    }
}
