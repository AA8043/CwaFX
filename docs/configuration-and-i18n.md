# 配置与国际化

## app.json

文件位置固定为 `src/main/resources/app.json`（打包后是 classpath 根目录）：

| 字段                 | 类型   | 默认值  | 说明                                 |
|----------------------|--------|---------|--------------------------------------|
| `defaultLanguage`    | 字符串 | `en_US` | 启动时使用的 `Locale`，例如 `zh_CN`  |
| `taskThreadPoolSize` | 整数   | `2`     | `Tasks` 固定线程池大小               |
| `settingsBaseDir`    | 路径   | `.`     | `@Settings` 文件的基础目录，支持 `~` |

## 语言资源

在 `src/main/resources/languages/` 创建 `messages.properties`（英文）和区域文件，例如 `messages_zh_CN.properties`：

```properties
window.title=示例应用
window.ready=窗口已准备就绪
modal.info=提示
modal.ok=确定
```

在代码中使用 `I18n.get("window.title")`。

## 设置持久化

`@Settings` 类必须是单例 Bean。启动第二阶段会从 JSON 文件填充对象，应用停止时将对象写回同一路径：

```java

@Bean
@Settings("settings.json")
public class UserSettings {
    private boolean darkMode;
    private int pageSize = 20;
}
```

实际文件位于 `settingsBaseDir/settings.json`。首次运行且文件不存在时使用字段默认值。

## 快捷键

通过 `KeyMappings` 注册快捷键，并在 Bean 方法上使用 `@OnKeyPressed`：

```java

@Autowired(name = "KeyMappings")
private KeyMappings keyMappings;

@Initialize
private void registerKeys() {
    keyMappings.add(new KeyMapping(
        "save", KeyCombination.keyCombination("Ctrl+S"), null));
}

@OnKeyPressed("save")
private void save() {
}
```

快捷键在窗口创建时绑定到该窗口的 `Scene`。`setKey` 会把用户修改后的组合键写入工作目录的 `keyMappings.json`。
