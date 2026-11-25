package org.a8043.cwaFX;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import org.a8043.cwaFX.events.DestroyEvent;

import java.util.Locale;

public class FXApp extends Application {
    @Getter(AccessLevel.PACKAGE)
    private static FXApp instance;

    public FXApp() {
        instance = this;
    }

    @Override
    public void start(Stage stage) {
        CwaFX cwaFX = CwaFX.getInstance();
        try {
            I18n.load(cwaFX.getClazz(), Locale.forLanguageTag(cwaFX.getConfig().getDefaultLanguage()));
            cwaFX.signalFxLoaded();
        } catch (Throwable throwable) {
            cwaFX.signalFxLoadFailure(throwable);
            javafx.application.Platform.exit();
            if (throwable instanceof RuntimeException runtimeException) throw runtimeException;
            if (throwable instanceof Error error) throw error;
            throw new IllegalStateException("Unable to initialize JavaFX", throwable);
        }
    }

    @Override
    public void stop() {
        CwaFX cwaFX = CwaFX.getInstance();
        if (cwaFX.isFxStartupPending()) {
            cwaFX.signalFxLoadFailure(new IllegalStateException("JavaFX stopped before application startup completed"));
        }
        CwaPlatform.markFxUnavailable();
        try {
            cwaFX.notifyEvent(new DestroyEvent());
        } finally {
            cwaFX.signalFxStopped();
        }
    }
}
