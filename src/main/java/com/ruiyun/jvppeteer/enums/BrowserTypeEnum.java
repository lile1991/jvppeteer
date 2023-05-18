package com.ruiyun.jvppeteer.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BrowserTypeEnum {
    CHROME("Chrome"),
    CHROMIUM("Chromium"),
    FIREFOX("Firefox"),
    MIMIC("Mimic");

    public final String browserName;

    public static BrowserTypeEnum enumOf(String browserName) {
        BrowserTypeEnum[] values = BrowserTypeEnum.values();
        for(BrowserTypeEnum v: values) {
            if(v.browserName.equals(browserName)) {
                return v;
            }
        }
        return null;
    }
}
