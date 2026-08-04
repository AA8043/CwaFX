package org.a8043.cwaFX;

import javafx.application.Application;
import javafx.stage.Stage;

public class FXApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        I18n.init(CwaFX.getInstance().getClazz());
        CwaFX.getInstance().getFxLoadLatch().countDown();
    }
}
