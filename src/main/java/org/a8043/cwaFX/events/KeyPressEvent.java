package org.a8043.cwaFX.events;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class KeyPressEvent extends Event {
    String name;
}
