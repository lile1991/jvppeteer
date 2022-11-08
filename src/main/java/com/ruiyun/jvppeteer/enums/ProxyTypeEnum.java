package com.ruiyun.jvppeteer.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ProxyTypeEnum {
    HTTP("http"),
    SOCKS5("socks5"),
    NO_PROXY("noproxy"),
    // 新加的
    LUMINATI("Luminati"),
    OXYLABS("Oxylabs"),
    _911("911");

    public final String type;

    public static ProxyTypeEnum enumOf(String code) {
        ProxyTypeEnum[] values = ProxyTypeEnum.values();
        for(ProxyTypeEnum value: values) {
            if(value.type.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}
