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
