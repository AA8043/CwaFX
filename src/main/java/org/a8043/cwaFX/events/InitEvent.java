package org.a8043.cwaFX.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class InitEvent extends Event {
    @Getter
    private final int sequence;
}
