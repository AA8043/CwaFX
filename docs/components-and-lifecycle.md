# 组件与生命周期

## Bean 扫描

框架启动后扫描启动类所在包。标注 `@Bean` 的类会被注册到 `AppContext`。`@Bean(single = true)`（默认值）按类型保持一个实例；设置为
`false` 时，可使用不同名称创建多个实例。

```java

@Bean(single = false)
public class PanelModel {
}
```

框架预注册了 `CwaFX`、`AppContext`、`WindowCreator`、`EventPublisher`、`KeyMappings`、`FXApp` 和 `Tasks`。这些 Bean 可以直接注入。

## 依赖注入

```java

@Autowired
private UserService userService;

@Autowired(name = "secondaryWindow")
private Window window;
```

不填写名称时，框架优先按类型匹配唯一 Bean；存在多个候选时应显式填写 `name`。名称会规范化为 camelCase。依赖稍后创建时，框架会在新
Bean 注册后再次尝试注入。

## 初始化和销毁

- `@Initialize`：依赖注入完成后调用一次，方法必须无参数。
- `@PreDestroy`：JavaFX 应用停止时调用一次，方法必须无参数。
- `@OnFXThread`：可附加在框架调用的方法上；若当前不在 JavaFX 线程，框架会切换到 JavaFX Application Thread。

```java

@Bean
public class Session {
    @Initialize
    private void load() {
    }

    @PreDestroy
    private void save() {
    }
}
```

方法查找基于当前类声明的方法，建议不要依赖继承的注解方法。

## 用户事件

继承 `org.a8043.cwaFX.userEvent.Event` 定义事件，通过预注册的 `EventPublisher` 发布：

```java
public final class UserLoggedIn extends Event {
    private final String userId;

    public UserLoggedIn(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}

@OnEvent(UserLoggedIn.class)
private void onLogin(UserLoggedIn event) {
}
```

```java

@Autowired(name = "EventPublisher")
private EventPublisher events;

private void publish() {
    events.publish(new UserLoggedIn("u-1001"));
}
```

事件处理方法应只有一个与事件类型匹配的参数。
