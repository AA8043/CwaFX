package org.a8043.cwaFX.events;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.a8043.cwaFX.BeanKey;

@EqualsAndHashCode(callSuper = true)
@Value
public class NewBeanEvent extends Event {
    BeanKey key;
    Object object;
}
