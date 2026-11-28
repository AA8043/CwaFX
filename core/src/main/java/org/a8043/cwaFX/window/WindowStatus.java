package org.a8043.cwaFX.window;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WindowStatus {
    private double width;
    private double height;
    private double x;
    private double y;
    private boolean maximized;
}
