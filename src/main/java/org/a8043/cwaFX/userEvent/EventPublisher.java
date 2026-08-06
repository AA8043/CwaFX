package org.a8043.cwaFX.userEvent;

import lombok.AllArgsConstructor;
import org.a8043.cwaFX.CwaFX;

@AllArgsConstructor
public class EventPublisher {
    private final CwaFX cwaFX;

    public void publish(Event event) {
        cwaFX.notifyEvent(new UserEventWrapper(event));
    }
}
