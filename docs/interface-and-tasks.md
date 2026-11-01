# 界面与任务

## FXML 视图

`@FxmlView` 会在对应类 Bean 创建时加载 FXML，并将根节点注册为 `Node` Bean。未指定 `fxml` 时，默认查找与类同包、同名的
`.fxml` 文件。

```java

@Bean
@FxmlView(value = "mainView", fxml = "/views/Main.fxml")
public class MainController {
    @FXML
    private Label status;
}
```

框架会把该 Bean 作为 controller。FXML 中的资源由 `I18n` 当前语言包提供。

## MaterialSwitch 开关

`MaterialSwitch` 与 `MaterialSpinner` 一样直接继承 `Region`，无需额外样式表即可使用。
开关本体默认尺寸为 52 × 32，可用 `new MaterialSwitch(40)` 指定高度并保持宽高比例，或通过布局尺寸缩放。
`defaultStyles/light.css` 和 `defaultStyles/dark.css` 已包含浅色、深色配色，以及文字、焦点和禁用样式。

```java
MaterialSwitch toggle = new MaterialSwitch("启用通知");
toggle.setText("自动保存"); // 显示在开关旁的说明文字，支持 textProperty() 绑定
toggle.setTooltip(new Tooltip("启用后自动保存修改")); // 鼠标悬停提示
toggle.setSelected(true);
toggle.selectedProperty().addListener((observable, oldValue, selected) -> {
    // 响应选中状态变化，也可以使用 bindBidirectional 与业务属性同步
});
toggle.setOnAction(event -> {
    // 响应用户操作或 fire()；setSelected() 不触发 ActionEvent
});
```

支持鼠标左键、空格键、回车键和辅助技术激活；禁用时不响应操作。
`selectedProperty()` 单向绑定时通过绑定源控制状态，双向绑定时可直接操作开关。
切换动画默认开启，可用 `setAnimated(false)` 关闭；隐藏、移出场景或禁用时立即收束到当前状态。

颜色可以通过对应的 `Paint` 属性或 CSS 配置：

```css
.material-switch {
    -fx-switch-color: #0071ff;                  /* 选中轨道 */
    -fx-switch-track-color: #e0e3e7;            /* 未选中轨道 */
    -fx-switch-thumb-color: white;             /* 选中滑块 */
    -fx-switch-unselected-thumb-color: #74777f; /* 未选中滑块与边框 */
}
```

说明文字会计入首选尺寸，点击文字同样可以切换状态；空文字不占用额外宽度。
`tooltipProperty()` 支持绑定，替换或清空 Tooltip 会自动卸载旧提示。

FXML 中可直接声明文字与悬停提示：

```xml
<?import org.a8043.cwaFX.components.MaterialSwitch?>
<?import javafx.scene.control.Tooltip?>
<MaterialSwitch text="自动保存" selected="true" animated="false">
    <tooltip>
        <Tooltip text="启用后自动保存修改"/>
    </tooltip>
</MaterialSwitch>
```

## 窗口、模态框和通知

```java
Window window = windowCreator.create("main", "window.title", 960, 640);
window.navigate("mainView", null);
window.show();

ModalController<Node> modal = window.showModal("详情", detailNode);
modal.setOnClose(() ->refresh());

NotificationController notice = window.showNotification("success", "操作完成", "数据已保存");
```

`showModal` 返回的 controller 可调用 `close()`；通知默认显示 3000 毫秒，可通过 `WindowCreator.setNotificationTime`
修改，设为小于等于 0 时不自动关闭。通知位置默认是右下角，可设置为 `NotificationLocation` 枚举值。

`WindowCreator` 默认加载 `defaultStyles/light.css`。可用 `setStyles`、`addStyle` 和 `removeStyle` 更新所有已创建窗口的样式；深色主题资源为
`WindowCreator.DARK_STYLE`。

## 页面导航

标注 `@FxmlView` 的控制器可以作为页面，通过注解名称在窗口内切换。控制器实现 `Page<T>` 后，会在页面每次变为可见前收到进入参数：

```java
@Bean
@FxmlView("detailPage")
public class DetailController implements Page<Long> {
    @Override
    public void onNavigate(Long userId) {
        // 根据 userId 刷新页面
    }
}
```

导航默认把当前页压入当前窗口的返回栈；首次导航会创建根页：

```java
window.navigate("homePage", null);
window.navigate("detailPage", 1001L, result -> refresh(result));
window.back("saved");
```

`replace` 会替换当前页但保留更早的历史：

```java
window.replace("editPage", 1001L);
```

可通过 `canGoBack()` 控制返回操作；根页调用 `back()` 会抛出 `IllegalStateException`。导航方法应在 JavaFX Application Thread 调用。

## 后台任务

使用 JavaFX `Task`，通过 `Tasks` 执行。线程池大小由 `taskThreadPoolSize` 控制：

```java

@Autowired(name = "Tasks")
private Tasks tasks;

Task<String> task = new Task<>() {
    @Override
    protected String call() throws Exception {
        updateTitle("导入数据");
        updateProgress(1, 2);
        return "完成";
    }
};
tasks.executeTask(task);
```

任务成功、失败或取消后会从 `tasks` 列表移除。需要展示任务列表时调用 `tasks.createView()`，返回可直接放入布局的 `TasksView`。
