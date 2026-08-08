package org.a8043.cwaFX.window;

import animatefx.animation.*;
import cn.hutool.core.util.ReflectUtil;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import org.a8043.cwaFX.I18n;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.events.KeyPressEvent;
import org.a8043.cwaFX.keyMapping.KeyMappings;

import java.util.Objects;
import java.util.function.Function;

@Bean(single = false)
public class Window {
    private final WindowCreator windowCreator;
    @Getter
    private final String name;
    @Getter
    private final Stage stage;
    @Getter
    private final StackPane pane = new StackPane();
    private final Scene scene;
    private VBox notificationContainer;
    @Getter
    private final WindowStatus status;

    Window(WindowCreator windowCreator, String name, Stage stage) {
        this.windowCreator = windowCreator;
        this.name = name;
        this.stage = stage;
        scene = new Scene(pane);
        status = new WindowStatus(stage.getWidth(), stage.getHeight(), stage.getX(), stage.getY(), stage.isMaximized());

        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            windowCreator.updateStatusJson(this);
            status.setWidth(newVal.doubleValue());
        });
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            windowCreator.updateStatusJson(this);
            status.setHeight(newVal.doubleValue());
        });
        stage.xProperty().addListener((obs, oldVal, newVal) -> {
            status.setX(newVal.doubleValue());
            windowCreator.updateStatusJson(this);
        });
        stage.yProperty().addListener((obs, oldVal, newVal) -> {
            status.setY(newVal.doubleValue());
            windowCreator.updateStatusJson(this);
        });
        stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            status.setMaximized(newVal);
            windowCreator.updateStatusJson(this);
        });

        windowCreator.getContext().getBean(KeyMappings.class, "KeyMappings")
            .getKeyMappings().forEach(keyMapping -> scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (keyMapping.getKey().match(event)) {
                    if (keyMapping.getOnlyIn() != null && scene.getFocusOwner() != keyMapping.getOnlyIn()) {
                        return;
                    }
                    windowCreator.getContext().getCwaFX().notifyEvent(new KeyPressEvent(keyMapping.getName()));
                }
            }));

        stage.setScene(scene);
        updateStyle();
        setup(pane);
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

    void updateStyle() {
        scene.getStylesheets().setAll(windowCreator.getStyles());
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

    private static void setup(Node node) {
        Function<ObservableList<Node>, Void> setupAndListen = list -> {
            list.forEach(Window::setup);
            list.addListener((ListChangeListener<Node>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        change.getAddedSubList().forEach(Window::setup);
                    }
                }
            });
            return null;
        };

        Function<TabPane, Void> setupTabPane = pane -> {
            pane.getTabs().forEach(tab -> setup(tab.getContent()));
            pane.getTabs().addListener((ListChangeListener<Tab>) change -> {
                while (change.next()) {
                    if (change.wasAdded()) {
                        change.getAddedSubList().forEach(tab -> setup(tab.getContent()));
                    }
                }
            });
            return null;
        };

        if (node instanceof TabPane pane) {
            setupTabPane.apply(pane);
        } else if (node instanceof SplitPane pane) {
            setupAndListen.apply(pane.getItems());
        } else if (node instanceof Pane pane) {
            setupAndListen.apply(pane.getChildren());
        }

        if (node instanceof Button button) {
            ObservableList<Node> children = ReflectUtil.invoke(button, "getChildren");
            button.setOnMousePressed(event -> {
                Circle ripple = new Circle(event.getX(), event.getY(), 0, Color.WHITE);
                ripple.setOpacity(0.5);
                Rectangle clip = new Rectangle(button.getWidth(), button.getHeight());
                clip.setArcWidth(10);
                clip.setArcHeight(10);
                button.setClip(clip);
                children.add(ripple);
                Timeline timeline = new Timeline(new KeyFrame(Duration.ZERO,
                    new KeyValue(ripple.radiusProperty(), 0),
                    new KeyValue(ripple.opacityProperty(), 0.6)),
                    new KeyFrame(Duration.millis(300), new KeyValue(ripple.radiusProperty(), Math.sqrt(
                        Math.pow(button.getWidth(), 2) + Math.pow(button.getHeight(), 2))),
                        new KeyValue(ripple.opacityProperty(), 0)));
                timeline.setOnFinished(e -> children.remove(ripple));
                timeline.play();
            });
        }
    }
}
