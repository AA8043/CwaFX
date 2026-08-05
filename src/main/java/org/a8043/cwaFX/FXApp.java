package org.a8043.cwaFX;

import javafx.application.Application;
import javafx.stage.Stage;
import org.a8043.cwaFX.events.DestroyEvent;

import java.util.Locale;

public class FXApp extends Application {
    @Override
    public void start(Stage stage) {
        I18n.load(CwaFX.getInstance().getClazz(), Locale.of(CwaFX.getInstance().getConfig().getDefaultLanguage()));
        CwaFX.getInstance().getFxLoadLatch().countDown();
    }

    @Override
    public void stop() {
        CwaFX.getInstance().notifyEvent(new DestroyEvent());
    }
}
