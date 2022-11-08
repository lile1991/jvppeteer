package com.ruiyun.jvppeteer.enums;

import lombok.AllArgsConstructor;

/**
 * IP类型
 */
@AllArgsConstructor
public enum ProxyIPTypeEnum {
    IPV4("ipv4"),
    IPV6("ipv6");

    public final String type;

    public static ProxyIPTypeEnum enumOf(String code) {
        ProxyIPTypeEnum[] values = ProxyIPTypeEnum.values();
        for(ProxyIPTypeEnum value: values) {
            if(value.type.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
