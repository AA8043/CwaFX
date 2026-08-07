package org.a8043.cwaFX;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
public class AppConfig {
    private String defaultLanguage = "en_US";
    private int taskThreadPoolSize = 2;
}
