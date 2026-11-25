package org.a8043.cwaFX;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;

import java.lang.reflect.Field;

@Value
@Getter
public class FieldAccessor {
    Field field;
    Object object;

    @SneakyThrows
    public void set(Object value) {
        field.setAccessible(true);
        field.set(object, value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FieldAccessor accessor && field == accessor.field && object == accessor.object;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(field) + System.identityHashCode(object);
    }
}
