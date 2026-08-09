# 快速开始

## 1. 创建项目

使用 Java 21 和 JavaFX 21 创建应用，并添加依赖：

Gradle：

```kotlin
repositories { mavenCentral() }
dependencies { implementation("io.github.aa8043:cwafx:{版本}") }
```

Maven：

```xml

<dependency>
    <groupId>io.github.aa8043</groupId>
    <artifactId>cwafx</artifactId>
    <version>{版本}</version>
</dependency>
```

JavaFX 至少需要 `javafx-controls` 和 `javafx-fxml`。如果使用模块化项目，请确保对应模块可读，并打开 FXML 控制器所在包供
`javafx.fxml` 反射访问。

## 2. 添加启动配置

在 `src/main/resources/app.json` 添加配置。CwaFX 启动时会从 classpath 根目录读取它；文件缺失或 JSON 无法解析时应用不会继续启动。

```json
{
    "defaultLanguage": "zh_CN",
    "taskThreadPoolSize": 4,
    "settingsBaseDir": "~/.my-javafx-app"
}
```

三个字段都有默认值：语言为 `en_US`，任务线程数为 `2`，设置目录为当前目录。`~` 会替换为当前用户目录。

## 3. 启动框架

传给 `CwaFX.start` 的 class 用来确定扫描包和资源所属模块：

```java
package com.example.app;

import org.a8043.cwaFX.CwaFX;

public final class MyApp {
    public static void main(String[] args) {
        CwaFX.start(MyApp.class, args);
    }
}
```

所有 `@Bean`、`@FxmlView` 和 `@Settings` 类应放在 `com.example.app` 或其子包中。

## 4. 创建并显示窗口

`WindowCreator` 是框架预注册的 Bean。窗口创建应在 JavaFX 线程执行：

```java

@Bean
public class MainWindow {
    @Autowired(name = "WindowCreator")
    private WindowCreator windows;

    @Initialize
    @OnFXThread
    private void open() {
        Window window = windows.create("main", "window.title", 960, 640);
        window.navigate("mainView", null);
        window.show();
    }
}
```

`WindowCreator.create` 的第一个参数是窗口 Bean 名称，第二个参数是国际化 key。窗口会被注册到 `AppContext`，可按
`Window.class` 和名称获取。`mainView` 是对应 `@FxmlView` 的页面名称。

## 5. 构建和运行

```bash
./gradlew build
```
