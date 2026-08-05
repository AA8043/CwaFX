package org.a8043.cwaFX.window;

import animatefx.animation.FadeIn;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.Getter;
import org.a8043.cwaFX.I18n;
import org.a8043.cwaFX.annotations.bean.Bean;

import java.util.Objects;

@Bean(single = false)
@Getter
public class Window {
    private final Stage stage;
    private final StackPane pane = new StackPane();

    Window(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(pane));
    }

    public void display(Node node) {
        Objects.requireNonNull(node, "The node to display must not be null.");
        if (pane.getChildren().isEmpty()) {
            pane.getChildren().add(node);
        } else {
            pane.getChildren().set(0, node);
        }
    }

    public <N extends Node> ModalController<N> showModal(String name, N node) {
        VBox modal = new VBox();
        modal.getStyleClass().add("modal");

        Button closeButton = new Button("x");
        closeButton.getStyleClass().add("modal-close-button");

        BorderPane titleBar = new BorderPane(new Label(name),
            null, closeButton, null, null);
        titleBar.setMaxHeight(5);
        modal.getChildren().addAll(titleBar, new Separator());

        modal.getChildren().add(node);

        AnchorPane modalPane = new AnchorPane(modal);
        modalPane.getStyleClass().add("modal-bg");
        ModalController<N> controller = new ModalController<>(this, node, modalPane);

        node.layoutBoundsProperty().addListener((obd, oldValue, newValue) -> {
            double x = (pane.getWidth() - newValue.getWidth()) / 2;
            AnchorPane.setRightAnchor(modal, pane.getWidth() - (x + newValue.getWidth()));
            AnchorPane.setLeftAnchor(modal, x);

            double y = (pane.getHeight() - newValue.getHeight()) / 2;
            AnchorPane.setTopAnchor(modal, y);
            AnchorPane.setBottomAnchor(modal, pane.getHeight() - (y + newValue.getHeight()));
        });

        closeButton.setOnAction(e -> controller.close());

        pane.getChildren().add(modalPane);
        new FadeIn(modalPane).play();
        return controller;
    }

    public void showTipModal(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setFont(new Font(12));
        Button button = new Button(I18n.get("modal.ok"));
        VBox box = new VBox(label, button);
        box.setAlignment(Pos.CENTER);
        ModalController<VBox> modalController = showModal(I18n.get("modal.info"), box);
        button.setOnAction(e -> modalController.close());
    }
}
