package org.a8043.cwaFX.components;

import javafx.animation.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
    private final Pane tabHeaderPane = new Pane(); // 用于精确定位 Indicator
    private final HBox tabBox = new HBox();
    private final HBox trailingBox = new HBox();   // 右侧扩展操作区（如放按钮）
    private final HBox leadingBox = new HBox();    // 左侧扩展区
    private final StackPane contentArea = new StackPane();

    // 贴合 M3 规范的圆角指示条
    private final Rectangle indicator = new Rectangle();
    private final Map<Tab, Button> tabButtonMap = new HashMap<>();

    // 动画曲线：Material 标准减速缓动
    private static final Interpolator M3_EASING = Interpolator.SPLINE(0.2, 0.0, 0.0, 1.0);
    private Timeline indicatorTimeline;

    public MaterialTabPane() {
        getStyleClass().add("material-tab-pane");
        initUI();
        initListeners();
    }

    private void initUI() {
        // 1. 设置指示条外观
        indicator.getStyleClass().add("tab-indicator");
        indicator.setHeight(3.0);
        indicator.setArcWidth(3.0);
        indicator.setArcHeight(3.0);
        indicator.setManaged(false); // 手动控制位置和宽度，避免干扰流式布局

        // 2. 标签栏布局
        tabBox.getStyleClass().add("tab-buttons-box");
        tabBox.setAlignment(Pos.CENTER_LEFT);

        // 使用 Pane 包裹 tabBox 和浮动的 indicator
        tabHeaderPane.getChildren().addAll(tabBox, indicator);

        // 绑定 tabBox 高度更新 indicator 的 Y 轴贴底位置
        tabBox.heightProperty().addListener((obs, oldV, newV) -> {
            indicator.setLayoutY(newV.doubleValue() - indicator.getHeight());
        });

        // 3. 头部工具栏（左侧扩展、标签页区、右侧扩展）
        headerBar.getStyleClass().add("tab-header-bar");
        headerBar.setLeft(leadingBox);
        headerBar.setCenter(tabHeaderPane);
        headerBar.setRight(trailingBox);

        leadingBox.setAlignment(Pos.CENTER_LEFT);
        trailingBox.setAlignment(Pos.CENTER_RIGHT);
        trailingBox.getStyleClass().add("tab-trailing-box");

        // 4. 内容展示区
        contentArea.getStyleClass().add("tab-content-area");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        getChildren().addAll(headerBar, contentArea);
    }

    private void initListeners() {
        // 监听 Tab 列表变化
        tabs.addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab tab : c.getAddedSubList()) {
                        createTabButton(tab);
                    }
                }
                if (c.wasRemoved()) {
                    for (Tab tab : c.getRemoved()) {
                        Button btn = tabButtonMap.remove(tab);
                        if (btn != null) tabBox.getChildren().remove(btn);
                    }
                }
            }
            if (getSelectedTab() == null && !tabs.isEmpty()) {
                selectTab(tabs.get(0), false);
            }
        });

        // 选中 Tab 切换
        selectedTab.addListener((obs, oldTab, newTab) -> {
            if (oldTab != null) {
                Button oldBtn = tabButtonMap.get(oldTab);
                if (oldBtn != null)
                    oldBtn.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), false);
            }
            if (newTab != null) {
                Button newBtn = tabButtonMap.get(newTab);
                if (newBtn != null) {
                    newBtn.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), true);
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

        // 如果是首个，默认选中
        if (tabs.size() == 1) {
            selectTab(tab, false);
            // 界面初次布局完毕后矫正指示条位置
            javafx.application.Platform.runLater(() -> animateIndicatorTo(btn, false));
        }
    }

    /**
     * 平滑移动指示条
     */
    private void animateIndicatorTo(Button targetBtn, boolean animate) {
        if (targetBtn == null) return;
        indicator.setVisible(true);

        double targetX = targetBtn.getLayoutX();
        double targetWidth = targetBtn.getWidth();

        // 如果宽度尚未计算（首次渲染前），延后计算
        if (targetWidth == 0) {
            javafx.application.Platform.runLater(() -> animateIndicatorTo(targetBtn, false));
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

        // Material 3 样式的弹性平滑滑动动画
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

    /**
     * 内容切换动画（淡入 + 轻微上移）
     */
    private void updateContent(Node newContent) {
        contentArea.getChildren().clear();
        if (newContent != null) {
            newContent.setOpacity(0.0);
            newContent.setTranslateY(6.0);
            contentArea.getChildren().add(newContent);

            // 淡入与上移动画
            FadeTransition fadeIn = new FadeTransition(Duration.millis(240), newContent);
            fadeIn.setToValue(1.0);

            TranslateTransition moveUp = new TranslateTransition(Duration.millis(240), newContent);
            moveUp.setToY(0);
            moveUp.setInterpolator(M3_EASING);

            new ParallelTransition(fadeIn, moveUp).play();
        }
    }

    public void selectTab(Tab tab, boolean animate) {
        this.selectedTab.set(tab);
    }

    // ================== 公共 API ==================

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

    /**
     * 允许向 Tab 栏右侧添加自定义节点（如“新建”、“设置”按钮）
     */
    public ObservableList<Node> getTrailingItems() {
        return trailingBox.getChildren();
    }

    /**
     * 允许向 Tab 栏左侧添加自定义节点
     */
    public ObservableList<Node> getLeadingItems() {
        return leadingBox.getChildren();
    }
}
