package com.ruiyun.jvppeteer.util;

public class TimeUtils {
    public static int getCurSecond() {
        return (int) (System.currentTimeMillis() / 1000);
    }
    public static int getSecondDiff(int start) {
        return (int) (System.currentTimeMillis() / 1000 - start);
    }
}
