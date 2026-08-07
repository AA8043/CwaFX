package org.a8043.cwaFX.window;

import cn.hutool.core.io.resource.ResourceUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.I18n;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
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

    public Window create(String name, String titleKey, int width, int height) {
        Stage stage = new Stage();
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setTitle(I18n.get(titleKey));
        Window window = new Window(this, stage);
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
}
