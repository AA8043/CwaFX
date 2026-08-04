package org.a8043.cwaFX.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.a8043.cwaFX.BeanKey;

@AllArgsConstructor
@Getter
@ToString
public class NewBeanEvent extends Event {
    private final BeanKey key;
    private final Object object;
}
