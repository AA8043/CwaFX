package org.a8043.cwaFX.window;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.CwaFX;
import org.a8043.cwaFX.I18n;
import org.a8043.cwaFX.events.DestroyEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WindowCreator {
    public static final String LIGHT_STYLE = ResourceUtil.getResource("defaultStyles/light.css").toExternalForm();
    public static final String DARK_STYLE = ResourceUtil.getResource("defaultStyles/dark.css").toExternalForm();

    @Getter(AccessLevel.PACKAGE)
    private final AppContext context;
    private final ObservableList<String> styles = FXCollections.observableArrayList(LIGHT_STYLE);
    private NotificationLocation notificationLocation = NotificationLocation.BOTTOM_RIGHT;
    private int notificationTime = 3000;
    private final List<Window> windows = new ArrayList<>();
    private JSONObject statusJson;

    public WindowCreator(AppContext context) {
        this.context = context;

        File statusFile = new File(context.getCwaFX().getConfig().getSettingsBaseDir(), "windowStatus.json");
        if (statusFile.exists()) {
            statusJson = new JSONObject(FileUtil.readUtf8String(statusFile));
        } else {
            statusJson = new JSONObject();
        }
        context.getCwaFX().addOnEvent(e -> {
            if (e instanceof DestroyEvent) {
                FileUtil.writeUtf8String(statusJson.toString(), statusFile);
            }
        });
    }

    void updateStatusJson(Window window) {
        statusJson.set(window.getName(), window.getStatus());
    }

    public Window create(String name, String titleKey, int defaultWidth, int defaultHeight) {
        Stage stage = new Stage();
        stage.setTitle(I18n.get(titleKey));

        if (statusJson.containsKey(name)) {
            WindowStatus status = statusJson.getJSONObject(name).toBean(WindowStatus.class);
            stage.setX(status.getX());
            stage.setY(status.getY());
            stage.setWidth(status.getWidth());
            stage.setHeight(status.getHeight());
            stage.setMaximized(status.isMaximized());
        } else {
            stage.setWidth(defaultWidth);
            stage.setHeight(defaultHeight);
        }

        Window window = new Window(this, name, stage);
        windows.add(window);
        context.addBean(new BeanKey(Window.class, name), window);
        return window;
    }

    public void setStyles(String... styles) {
        this.styles.setAll(styles);
        windows.forEach(Window::updateStyle);
    }

    public void addStyle(String style) {
        styles.add(style);
        windows.forEach(Window::updateStyle);
    }

    public void removeStyle(String style) {
        styles.remove(style);
        windows.forEach(Window::updateStyle);
    }

    public static Window findByScene(Scene scene) {
        return CwaFX.getInstance().getContext().getBean(WindowCreator.class, "WindowCreator").getWindows()
            .stream().filter(window -> window.getStage().getScene() == scene).findFirst().orElse(null);
    }
}
