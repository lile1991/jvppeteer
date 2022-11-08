package com.ruiyun.jvppeteer.core.browser;

import lombok.Data;

@Data
public class Bounds {
    public final static String fullscreen = "fullscreen";
    public final static String maximized = "maximized";
    public final static String normal = "normal";
    public final static String minimized = "minimized";

    private Integer left;
    private Integer top;
    private Integer width;
    private Integer height;
    private String windowState = "normal";

    public Bounds(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    public Bounds(String windowState) {
        this.windowState = windowState;
    }

    public Bounds() {
    }
}
