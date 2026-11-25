package org.a8043.cwaFX.components;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class MaterialTabPane extends VBox {
    private final ObservableList<Tab> tabs = FXCollections.observableArrayList();
    private final ObjectProperty<Tab> selectedTab = new SimpleObjectProperty<>();
    private final BorderPane headerBar = new BorderPane();
    private final Pane tabHeaderPane = new Pane();
    private final HBox tabBox = new HBox();
    private final HBox trailingBox = new HBox();
    private final HBox leadingBox = new HBox();
    private final StackPane contentArea = new StackPane();

    private final Rectangle indicator = new Rectangle();
    private final Map<Tab, Button> tabButtonMap = new HashMap<>();

    private static final Interpolator M3_EASING = Interpolator.SPLINE(0.2, 0.0, 0.0, 1.0);
    private Timeline indicatorTimeline;

    public MaterialTabPane() {
        getStyleClass().add("material-tab-pane");
        initUI();
        initListeners();
    }

    private void initUI() {
        indicator.getStyleClass().add("tab-indicator");
        indicator.setHeight(3.0);
        indicator.setArcWidth(3.0);
        indicator.setArcHeight(3.0);
        indicator.setManaged(false);

        tabBox.getStyleClass().add("tab-buttons-box");
        tabBox.setAlignment(Pos.CENTER_LEFT);

        tabHeaderPane.getChildren().addAll(tabBox, indicator);

        tabBox.heightProperty().addListener((obs, oldV, newV) -> {
            indicator.setLayoutY(newV.doubleValue() - indicator.getHeight());
        });

        headerBar.getStyleClass().add("tab-header-bar");
        headerBar.setLeft(leadingBox);
        headerBar.setCenter(tabHeaderPane);
        headerBar.setRight(trailingBox);

        leadingBox.setAlignment(Pos.CENTER_LEFT);
        trailingBox.setAlignment(Pos.CENTER_RIGHT);
        trailingBox.getStyleClass().add("tab-trailing-box");

        contentArea.getStyleClass().add("tab-content-area");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        getChildren().addAll(headerBar, contentArea);
    }

    private void initListeners() {
        tabs.addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab tab : c.getAddedSubList()) {
                        createTabButton(tab);
                        tab.contentProperty().addListener((obs, oldContent, newContent) -> {
                            if (tab == getSelectedTab()) updateContent(newContent);
                        });
                    }
                }
                if (c.wasRemoved()) {
                    for (Tab tab : c.getRemoved()) {
                        Button btn = tabButtonMap.remove(tab);
                        if (btn != null) tabBox.getChildren().remove(btn);
                        if (tab == getSelectedTab()) {
                            int index = Math.min(Math.max(0, c.getFrom()), tabs.size() - 1);
                            selectedTab.set(tabs.isEmpty() ? null : tabs.get(index));
                        }
                    }
                }
            }
            if ((getSelectedTab() == null || !tabs.contains(getSelectedTab())) && !tabs.isEmpty()) {
                selectTab(tabs.getFirst(), false);
            } else if (tabs.isEmpty()) {
                selectedTab.set(null);
            }
            syncSelectedTab();
        });

        selectedTab.addListener((obs, oldTab, newTab) -> {
            if (oldTab != null) {
                Button oldBtn = tabButtonMap.get(oldTab);
                if (oldBtn != null)
                    oldBtn.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), false);
            }
            if (newTab != null) {
                Button newBtn = tabButtonMap.get(newTab);
                if (newBtn != null) {
                    newBtn.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
                    animateIndicatorTo(newBtn, true);
                }
                updateContent(newTab.getContent());
            } else {
                contentArea.getChildren().clear();
                indicator.setVisible(false);
            }
        });
    }

    private void createTabButton(Tab tab) {
        Button btn = new Button();
        btn.getStyleClass().add("material-tab-button");
        btn.textProperty().bind(tab.textProperty());
        btn.graphicProperty().bind(tab.graphicProperty());

        btn.setOnAction(e -> selectTab(tab, true));

        tabButtonMap.put(tab, btn);
        tabBox.getChildren().add(btn);
        btn.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0 && tab == getSelectedTab()) animateIndicatorTo(btn, false);
        });

        if (tabs.size() == 1) {
            selectTab(tab, false);
            Platform.runLater(() -> animateIndicatorTo(btn, false));
        }
    }

    private void animateIndicatorTo(Button targetBtn, boolean animate) {
        if (targetBtn == null) return;
        indicator.setVisible(true);

        double targetX = targetBtn.getLayoutX();
        double targetWidth = targetBtn.getWidth();

        if (targetWidth == 0) {
            indicator.setVisible(false);
            return;
        }

        if (indicatorTimeline != null) {
            indicatorTimeline.stop();
        }

        if (!animate) {
            indicator.setLayoutX(targetX);
            indicator.setWidth(targetWidth);
            return;
        }

        indicatorTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(indicator.layoutXProperty(), indicator.getLayoutX()),
                new KeyValue(indicator.widthProperty(), indicator.getWidth())),
            new KeyFrame(Duration.millis(260),
                new KeyValue(indicator.layoutXProperty(), targetX, M3_EASING),
                new KeyValue(indicator.widthProperty(), targetWidth, M3_EASING))
        );
        indicatorTimeline.play();
    }

    private void updateContent(Node newContent) {
        contentArea.getChildren().clear();
        if (newContent != null) {
            newContent.setOpacity(0.0);
            newContent.setTranslateY(6.0);
            contentArea.getChildren().add(newContent);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(240), newContent);
            fadeIn.setToValue(1.0);

            TranslateTransition moveUp = new TranslateTransition(Duration.millis(240), newContent);
            moveUp.setToY(0);
            moveUp.setInterpolator(M3_EASING);

            new ParallelTransition(fadeIn, moveUp).play();
        }
    }

    public void selectTab(Tab tab, boolean animate) {
        if (tab != null && !tabs.contains(tab)) {
            throw new IllegalArgumentException("The selected tab is not part of this pane.");
        }
        this.selectedTab.set(tab);
        if (tab != null) {
            Button button = tabButtonMap.get(tab);
            if (button != null) animateIndicatorTo(button, animate);
        }
    }

    private void syncSelectedTab() {
        Tab selected = getSelectedTab();
        for (Map.Entry<Tab, Button> entry : tabButtonMap.entrySet()) {
            entry.getValue().pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), entry.getKey() == selected);
        }
        if (selected == null) {
            contentArea.getChildren().clear();
            indicator.setVisible(false);
        } else {
            updateContent(selected.getContent());
            animateIndicatorTo(tabButtonMap.get(selected), false);
        }
    }

    public ObservableList<Tab> getTabs() {
        return tabs;
    }

    public ObjectProperty<Tab> selectedTabProperty() {
        return selectedTab;
    }

    public Tab getSelectedTab() {
        return selectedTab.get();
    }

    public void setSelectedTab(Tab tab) {
        selectTab(tab, true);
    }

    public ObservableList<Node> getTrailingItems() {
        return trailingBox.getChildren();
    }

    public ObservableList<Node> getLeadingItems() {
        return leadingBox.getChildren();
    }
}
