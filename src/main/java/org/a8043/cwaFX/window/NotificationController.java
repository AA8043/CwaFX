package org.a8043.cwaFX.window;

import javafx.animation.PauseTransition;
import javafx.scene.layout.Region;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class NotificationController {
    private final Window window;
    @Getter
    private final Region node;
    @Getter
    private final NotificationLocation location;
    private final PauseTransition timer;
    @Setter
    @Getter
    private Runnable onClose;
    private boolean closed = false;

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (timer != null) {
            timer.stop();
        }
        window.removeNotification(this);
        if (onClose != null) {
            onClose.run();
        }
    }
}
