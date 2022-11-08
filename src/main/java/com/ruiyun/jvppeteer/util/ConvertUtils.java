package com.ruiyun.jvppeteer.util;

public class ConvertUtils {
    public static Integer toInteger(String str) {
        if(str == null) {
            return 0;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static <T> T defaultValue(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
