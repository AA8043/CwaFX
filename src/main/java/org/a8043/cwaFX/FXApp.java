package org.a8043.cwaFX;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import org.a8043.cwaFX.events.DestroyEvent;

import java.util.Locale;
import java.util.Objects;

public class FXApp extends Application {
    @Getter(AccessLevel.PACKAGE)
    private static FXApp instance;

    public FXApp() {
        instance = this;
    }

    @Override
    public void start(Stage stage) {
        I18n.load(CwaFX.getInstance().getClazz(), Locale.of(CwaFX.getInstance().getConfig().getDefaultLanguage()));
        CwaFX.getInstance().getFxLoadLatch().countDown();
    }

    @Override
    public void stop() {
        CwaFX.getInstance().notifyEvent(new DestroyEvent());
    }

    public void setStyle(String style) {
        setUserAgentStylesheet(Objects.requireNonNull(CwaFX.getInstance().getClazz().getResource(style)).toExternalForm());
    }
}
