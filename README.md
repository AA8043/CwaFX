# CwaFX

CwaFX 是一个面向 JavaFX 21 的轻量级应用框架，提供组件扫描与依赖注入、FXML 视图、窗口辅助类、国际化、设置持久化、任务管理和快捷键事件等基础能力。项目使用
Java 21。

## 特性

- 基于 `@Bean` 和 `@Autowired` 的组件管理与依赖注入
- `@Initialize`、`@PreDestroy`、`@OnEvent`、`@OnKeyPressed` 生命周期和事件回调
- `@FxmlView` 自动加载 FXML 并注册为 `Node` Bean
- `WindowCreator`/`Window` 提供窗口、模态框、通知和默认样式支持
- `Tasks` 统一管理 JavaFX `Task`，提供可绑定的任务列表视图
- `@Settings` 从 JSON 文件加载并在退出时保存设置
- 基于 `ResourceBundle` 的国际化和可配置的默认语言

## 环境要求

- JDK 21 或更高版本
- JavaFX 21（`controls`、`fxml` 模块）

## 安装

发布坐标为 Maven Central：

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.aa8043:cwafx:{版本}")
}
```

使用 Maven 时：

```xml

<dependency>
    <groupId>io.github.aa8043</groupId>
    <artifactId>cwafx</artifactId>
    <version>{版本}</version>
</dependency>
```

## 最小示例

在 `src/main/resources/app.json` 放置启动配置（该文件是必需的）：

```json
{
    "defaultLanguage": "zh_CN",
    "taskThreadPoolSize": 2,
    "settingsBaseDir": "~/.my-javafx-app"
}
```

准备一个位于业务包根部的启动类：

```java
package com.example.app;

import org.a8043.cwaFX.CwaFX;

public final class MyApp {
    public static void main(String[] args) {
        CwaFX.start(MyApp.class, args);
    }
}
```

框架会扫描 `MyApp` 所在包及其子包。一个可注入的组件示例：

```java
package com.example.app;

import org.a8043.cwaFX.annotations.bean.Autowired;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.annotations.bean.Initialize;

@Bean
public class GreetingService {
    @Autowired(name = "windowCreator")
    private org.a8043.cwaFX.window.WindowCreator windowCreator;

    @Initialize
    private void initialize() {
        // 依赖注入完成后执行
    }
}
```

完整接入说明请参阅 [`docs/quick-start.md`](docs/quick-start.md)。

## 文档

- [`docs/quick-start.md`](docs/quick-start.md)：从依赖、资源到第一个窗口
- [`docs/components-and-lifecycle.md`](docs/components-and-lifecycle.md)：Bean、注入、生命周期和事件注解
- [`docs/forms.md`](docs/forms.md)：动态表单、校验、提交事件和 FXML 声明
- [`docs/interface-and-tasks.md`](docs/interface-and-tasks.md)：窗口、FXML、模态框、通知和后台任务
- [`docs/configuration-and-i18n.md`](docs/configuration-and-i18n.md)：`app.json`、设置文件、语言资源和快捷键

## 构建和测试

```bash
./gradlew build
```

Windows 可执行：

```powershell
.\gradlew.bat build
```

## 许可证

本项目采用 [GNU GPL v3.0](LICENSE) 许可证。
