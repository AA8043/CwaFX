package org.a8043.cwaFX;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.Locale;

@Getter
@Setter(AccessLevel.PRIVATE)
public class AppConfig {
    private String defaultLanguage = Locale.getDefault().getLanguage();
    private int taskThreadPoolSize = 2;
    private File settingsBaseDir = new File(".");
}
