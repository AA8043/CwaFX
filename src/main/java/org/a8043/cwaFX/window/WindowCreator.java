package org.a8043.cwaFX.window;

import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.I18n;

@RequiredArgsConstructor
public class WindowCreator {
    private final AppContext context;
    @Getter
    @Setter
    private NotificationLocation notificationLocation = NotificationLocation.BOTTOM_RIGHT;
    @Getter
    @Setter
    private int notificationTime = 3000;

    public Window create(String name, String titleKey, int width, int height) {
        Stage stage = new Stage();
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setTitle(I18n.get(titleKey));
        Window window = new Window(this, stage);
        context.addBean(new BeanKey(Window.class, name), window);
        return window;
    }
}
