package com.ruiyun.jvppeteer.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BrowserEnum {
    CHROME("Chrome", "Chrome", "Chrome.png"),
    MULTILOGIN("Multilogin", "Multilogin", "Mimic.png"),
    CHROMIUM("Chromium", "Chromium", "Chromium.png"),
    MULTILOGIN2("Multilogin2", "Mimic7", "Mimic.png"),
    MULTILOGIN80("Multilogin80", "Mimic8", "Mimic2.png");

    public final String browserName;
    public final String aliasName;
    public final String icon;

    public static BrowserEnum enumOf(String browserName) {
        BrowserEnum[] values = BrowserEnum.values();
        for(BrowserEnum v: values) {
            if(v.browserName.equals(browserName)) {
                return v;
            }
        }
        return null;
    }
}
