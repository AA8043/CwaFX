package org.a8043.cwaFX.window;

import animatefx.animation.AnimationFX;
import animatefx.animation.FadeIn;
import animatefx.animation.FadeInDown;
import animatefx.animation.FadeInLeft;
import animatefx.animation.FadeInRight;
import animatefx.animation.FadeInUp;
import animatefx.animation.FadeOutDown;
import animatefx.animation.FadeOutLeft;
import animatefx.animation.FadeOutRight;
import animatefx.animation.FadeOutUp;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import org.a8043.cwaFX.I18n;
import org.a8043.cwaFX.annotations.bean.Bean;

import java.util.Objects;

@Bean(single = false)
public class Window {
    private final WindowCreator windowCreator;
    @Getter
    private final Stage stage;
    @Getter
    private final StackPane pane = new StackPane();
    private VBox notificationContainer;

    Window(WindowCreator windowCreator, Stage stage) {
        this.windowCreator = windowCreator;
        this.stage = stage;
        stage.setScene(new Scene(pane));
    }

    public void show() {
        stage.show();
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

    public NotificationController showNotification(String name, String title, String content) {
        NotificationLocation location = windowCreator.getNotificationLocation();
        VBox container = getNotificationContainer(location);

        VBox card = buildNotificationCard(name, title, content);

        PauseTransition timer = null;
        int time = windowCreator.getNotificationTime();
        if (time > 0) {
            timer = new PauseTransition(Duration.millis(time));
        }

        NotificationController controller = new NotificationController(this, card, location, timer);

        Button closeButton = (Button) card.getProperties().get("closeButton");
        closeButton.setOnAction(e -> controller.close());

        if (timer != null) {
            timer.setOnFinished(e -> controller.close());
        }

        if (isTop(location)) {
            container.getChildren().addFirst(card);
        } else {
            container.getChildren().add(card);
        }

        entranceAnimation(card, location).play();
        if (timer != null) {
            timer.play();
        }
        return controller;
    }

    private VBox getNotificationContainer(NotificationLocation location) {
        if (notificationContainer == null) {
            notificationContainer = new VBox(10);
            notificationContainer.getStyleClass().add("notification-container");
            notificationContainer.setPadding(new Insets(16));
            notificationContainer.setPickOnBounds(false);
            notificationContainer.setMaxWidth(Region.USE_PREF_SIZE);
            notificationContainer.setMaxHeight(Region.USE_PREF_SIZE);
        }
        notificationContainer.setAlignment(toPos(location));
        StackPane.setAlignment(notificationContainer, toPos(location));
        if (!pane.getChildren().contains(notificationContainer)) {
            pane.getChildren().add(notificationContainer);
        }
        notificationContainer.toFront();
        return notificationContainer;
    }

    void removeNotification(NotificationController controller) {
        Region card = controller.getNode();
        if (notificationContainer == null || !notificationContainer.getChildren().contains(card)) {
            return;
        }
        AnimationFX exit = exitAnimation(card, controller.getLocation());
        exit.setOnFinished(e -> notificationContainer.getChildren().remove(card));
        exit.play();
    }

    private VBox buildNotificationCard(String name, String title, String content) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("notification-title");

        Button closeButton = new Button("x");
        closeButton.getStyleClass().add("modal-close-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
            header.getChildren().add(new ImageView() {{
                getStyleClass().add("notification-icon-" + name);
            }});
        header.getChildren().addAll(titleLabel, spacer, closeButton);

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add("notification-content");
        contentLabel.setWrapText(true);

        VBox card = new VBox(4, header, contentLabel);
        card.getStyleClass().add("modal");
        card.setPadding(new Insets(10));
        card.setPrefWidth(300);
        card.setMaxWidth(Region.USE_PREF_SIZE);
        card.getProperties().put("closeButton", closeButton);
        return card;
    }

    private static boolean isTop(NotificationLocation location) {
        return location == NotificationLocation.TOP_LEFT
            || location == NotificationLocation.TOP_RIGHT
            || location == NotificationLocation.TOP_CENTER;
    }

    private static Pos toPos(NotificationLocation location) {
        return switch (location) {
            case TOP_LEFT -> Pos.TOP_LEFT;
            case TOP_RIGHT -> Pos.TOP_RIGHT;
            case BOTTOM_LEFT -> Pos.BOTTOM_LEFT;
            case BOTTOM_RIGHT -> Pos.BOTTOM_RIGHT;
            case TOP_CENTER -> Pos.TOP_CENTER;
            case BOTTOM_CENTER -> Pos.BOTTOM_CENTER;
        };
    }

    private static AnimationFX entranceAnimation(Node node, NotificationLocation location) {
        return switch (location) {
            case TOP_LEFT, BOTTOM_LEFT -> new FadeInLeft(node);
            case TOP_RIGHT, BOTTOM_RIGHT -> new FadeInRight(node);
            case TOP_CENTER -> new FadeInDown(node);
            case BOTTOM_CENTER -> new FadeInUp(node);
        };
    }

    private static AnimationFX exitAnimation(Node node, NotificationLocation location) {
        return switch (location) {
            case TOP_LEFT, BOTTOM_LEFT -> new FadeOutLeft(node);
            case TOP_RIGHT, BOTTOM_RIGHT -> new FadeOutRight(node);
            case TOP_CENTER -> new FadeOutUp(node);
            case BOTTOM_CENTER -> new FadeOutDown(node);
        };
    }
}
