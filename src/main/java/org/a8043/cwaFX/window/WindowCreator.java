package org.a8043.cwaFX.window;

import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.I18n;

@RequiredArgsConstructor
public class WindowCreator {
    private final AppContext context;

    public Window create(String name, String titleKey) {
        Stage stage = new Stage();
        stage.setTitle(I18n.get(titleKey));
        Window window = new Window(stage);
        context.addBean(new BeanKey(Window.class, name), window);
        return window;
    }
}
