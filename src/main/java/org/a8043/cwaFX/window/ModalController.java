package org.a8043.cwaFX.window;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ModalController<N extends Node> {
    private final Window window;
    @Getter
    private final N node;
    @Getter
    private final AnchorPane modalPane;
    @Setter
    @Getter
    private Runnable onClose;

    private boolean closing;

    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        window.closeModal(this);
    }

    void finishClose() {
        window.getPane().getChildren().remove(modalPane);
        if (onClose != null) {
            onClose.run();
        }
    }
}
