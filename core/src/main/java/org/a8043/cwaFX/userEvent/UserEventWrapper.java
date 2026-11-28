package org.a8043.cwaFX.userEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.a8043.cwaFX.events.Event;

@AllArgsConstructor
@Getter
public class UserEventWrapper extends Event {
    private final org.a8043.cwaFX.userEvent.Event event;
}
