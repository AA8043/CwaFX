package org.a8043.cwaFX.window;

import animatefx.animation.*;
import cn.hutool.core.util.ReflectUtil;
import javafx.animation.*;
import javafx.application.Platform;
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
import org.a8043.cwaFX.navigation.PageRegistry;
import org.a8043.cwaFX.navigation.WindowNavigation;

import java.util.Objects;
import java.util.function.Consumer;
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
    private final WindowNavigation navigation;

    Window(WindowCreator windowCreator, String name, Stage stage) {
        this.windowCreator = windowCreator;
        this.name = name;
        this.stage = stage;
        scene = new Scene(pane);
        status = new WindowStatus(stage.getWidth(), stage.getHeight(), stage.getX(), stage.getY(), stage.isMaximized());
        navigation = new WindowNavigation(windowCreator.getContext().getBean(PageRegistry.class, "PageRegistry"),
            this::displayContent);

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

    public <T> void navigate(String pageName, T parameter) {
        navigation.navigate(pageName, parameter);
    }

    public <T, R> void navigate(String pageName, T parameter, Consumer<? super R> onResult) {
        navigation.navigate(pageName, parameter, onResult);
    }

    public <T> void replace(String pageName, T parameter) {
        navigation.replace(pageName, parameter);
    }

    public void back() {
        navigation.back();
    }

    public <R> void back(R result) {
        navigation.back(result);
    }

    public boolean canGoBack() {
        return navigation.canGoBack();
    }

    private void displayContent(Node node) {
        Objects.requireNonNull(node, "The node to display must not be null.");
        if (pane.getChildren().isEmpty()) {
            pane.getChildren().add(node);
        } else {
            pane.getChildren().set(0, node);
        }
    }

    private static final Duration MODAL_OPEN_DURATION  = Duration.millis(220);
    private static final Duration MODAL_CLOSE_DURATION = Duration.millis(180);

    public <N extends Node> ModalController<N> showModal(String name, N node) {
        VBox modal = new VBox();
        modal.getStyleClass().add("modal");

        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("modal-close-button");

        Label titleLabel = new Label(name);
        titleLabel.getStyleClass().add("modal-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox titleBar = new HBox(titleLabel, spacer, closeButton);
        titleBar.getStyleClass().add("modal-header");
        titleBar.setAlignment(Pos.CENTER_LEFT);

        VBox contentBox = new VBox(node);
        contentBox.getStyleClass().add("modal-content");

        modal.getChildren().addAll(titleBar, contentBox);

        AnchorPane modalPane = new AnchorPane(modal);
        modalPane.getStyleClass().add("modal-bg");
        ModalController<N> controller = new ModalController<>(this, node, modalPane);

        Runnable recenter = () -> {
            double w = modal.getWidth();
            double h = modal.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            AnchorPane.setLeftAnchor(modal, (pane.getWidth() - w) / 2);
            AnchorPane.setTopAnchor(modal, (pane.getHeight() - h) / 2);
        };

        modal.widthProperty().addListener((obs, o, n) -> recenter.run());
        modal.heightProperty().addListener((obs, o, n) -> recenter.run());
        pane.widthProperty().addListener((obs, o, n) -> recenter.run());
        pane.heightProperty().addListener((obs, o, n) -> recenter.run());

        closeButton.setOnAction(e -> controller.close());

        modalPane.setOpacity(0);
        pane.getChildren().add(modalPane);

        pane.applyCss();
        pane.layout();
        recenter.run();

        Platform.runLater(() -> {
            recenter.run();
            buildOpenAnimation(modalPane, modal).play();
        });

        return controller;
    }

    private static Animation buildOpenAnimation(AnchorPane modalPane, Node modal) {
        FadeTransition bg = new FadeTransition(MODAL_OPEN_DURATION, modalPane);
        bg.setFromValue(0);
        bg.setToValue(1);

        Timeline content = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(modal.scaleXProperty(), 0.94, Interpolator.EASE_OUT),
                new KeyValue(modal.scaleYProperty(), 0.94, Interpolator.EASE_OUT),
                new KeyValue(modal.opacityProperty(), 0, Interpolator.EASE_OUT)),
            new KeyFrame(MODAL_OPEN_DURATION,
                new KeyValue(modal.scaleXProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(modal.scaleYProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(modal.opacityProperty(), 1, Interpolator.EASE_OUT))
        );

        return new ParallelTransition(bg, content);
    }

    void closeModal(ModalController<?> controller) {
        AnchorPane modalPane = controller.getModalPane();

        if (!pane.getChildren().contains(modalPane)) {
            controller.finishClose();
            return;
        }

        Node modal = modalPane.getChildren().isEmpty() ? null : modalPane.getChildren().get(0);

        FadeTransition bg = new FadeTransition(MODAL_CLOSE_DURATION, modalPane);
        bg.setToValue(0);

        Animation closeAnim;
        if (modal == null) {
            closeAnim = bg;
        } else {
            Timeline content = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(modal.scaleXProperty(), modal.getScaleX(), Interpolator.EASE_IN),
                    new KeyValue(modal.scaleYProperty(), modal.getScaleY(), Interpolator.EASE_IN),
                    new KeyValue(modal.opacityProperty(), modal.getOpacity(), Interpolator.EASE_IN)),
                new KeyFrame(MODAL_CLOSE_DURATION,
                    new KeyValue(modal.scaleXProperty(), 0.94, Interpolator.EASE_IN),
                    new KeyValue(modal.scaleYProperty(), 0.94, Interpolator.EASE_IN),
                    new KeyValue(modal.opacityProperty(), 0, Interpolator.EASE_IN))
            );
            closeAnim = new ParallelTransition(bg, content);
        }

        closeAnim.setOnFinished(e -> controller.finishClose());
        closeAnim.play();
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

        Button closeButton = new Button("×");
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
