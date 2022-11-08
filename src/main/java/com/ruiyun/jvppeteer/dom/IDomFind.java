package com.ruiyun.jvppeteer.dom;

import com.ruiyun.jvppeteer.util.ConvertUtils;
import com.ruiyun.jvppeteer.util.ListExt;
import com.ruiyun.jvppeteer.util.ThreadUtils;
import com.ruiyun.jvppeteer.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public abstract class IDomFind {
    public final String[] nonReplaces = new String[] {"{}"};

    public abstract Object evaluate(String pageFunction, Object... args);
    public abstract DomElement $(String selector);
    public abstract DomElement $(String selector, int timeout);
    public abstract ListExt<DomElement> $$(String selector);
    public abstract ListExt<DomElement> $$(String selector, int timeout);
    public DomElement $(String selector, String... replaces) {
        return $$(selector, replaces).first();
    }
    public DomElement $(String selector, int timeout, String... replaces) {
        for (int i = 0; i < timeout; i ++) {
            DomElement domElement = $(selector, replaces);
            if(domElement != null) {
                return domElement;
            }
            if(i >= timeout - 1) {
                return null;
            }
            ThreadUtils.sleep(1000);
        }
        return null;
    }
    public abstract ListExt<DomElement> $$(String selector, String... replaces);

    public DomElement jQuery(String selector, String... replaces) {
        return jQuery(selector, 0, replaces);
    }
    public DomElement jQuery(String selector, int timeout, String... replaces) {
        return jQueries(selector, timeout, replaces).first();
    }

    public ListExt<DomElement> jQueries(String selector, String... replaces) {
        return jQueries(selector, 0, replaces);
    }
    public ListExt<DomElement> jQueries(String selector, int timeout, String... replaces) {
        int start = TimeUtils.getCurSecond();

        if(replaces == null || replaces.length == 0) {
            replaces = nonReplaces;
        }
        String selectorDescription = selector + Arrays.asList(replaces);

        String rnId = DomElement.randomRnId();
        o: do {
            for (String replace : replaces) {
                try {
                    String isoSelector = selector.replace("{}", replace);
                    Boolean aBoolean = ConvertUtils.defaultValue((Boolean) evaluate("runnerNS.markRnId(null, \"" + isoSelector + "\", '" + rnId + "')"), false);
                    log.debug("执行jQuery选择器: {}, 查询结果: {}", isoSelector, aBoolean);
                    if (aBoolean) {
                        break o;
                    }
                } catch (Exception e) {
                    log.error("标记rnId异常", e);
                }
            }

            int secondDiff = TimeUtils.getSecondDiff(start);
            log.debug("重试获取{}， 已耗时{}s, 超时时间{}s", selectorDescription, secondDiff, timeout);
            if(timeout == 0 || secondDiff > timeout) {
                log.info("执行jQuery选择器 {} 超时， 已耗时{}s, 超时时间{}s", selectorDescription, secondDiff, timeout);
                return ListExt.emptyList();
            }
            ThreadUtils.sleep(1000);
        } while (true);

        return $$("[rn-id='" + rnId + "']");
    }
}
