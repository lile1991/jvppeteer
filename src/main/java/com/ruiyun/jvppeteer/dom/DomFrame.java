package com.ruiyun.jvppeteer.dom;

import com.ruiyun.jvppeteer.core.browser.MultipleBrowser;
import com.ruiyun.jvppeteer.core.page.Frame;
import com.ruiyun.jvppeteer.core.page.JSHandle;
import com.ruiyun.jvppeteer.core.page.Page;
import com.ruiyun.jvppeteer.util.CollectionUtils;
import com.ruiyun.jvppeteer.util.ListExt;
import com.ruiyun.jvppeteer.util.ThreadUtils;
import com.ruiyun.jvppeteer.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;

@Slf4j
public class DomFrame extends IDomFind {
    private final MultipleBrowser multipleBrowser;
    public final Page page;
    public final Frame frame;

    public DomFrame(MultipleBrowser multipleBrowser, Page page, Frame frame) {
        this.multipleBrowser = multipleBrowser;
        this.page = page;
        this.frame = frame;
    }


    public DomElement $(String selector) {
        return DomElement.$(multipleBrowser, page, frame, selector, 0);
    }

    public DomElement $(String selector, int timeout) {
        int start = TimeUtils.getCurSecond();
        for(int i = 0; i == 0 || i < timeout; i ++) {
            DomElement domElement = $(selector);
            int consumerSecond = TimeUtils.getSecondDiff(start);
            if(domElement != null) {
                log.debug("找到子元素{}， 耗时{}s, 超时时间{}s", selector, consumerSecond, timeout);
                return domElement;
            }

            if(consumerSecond < timeout) {
                if(i % 5 == 0) {
                    log.debug("等待子元素{}， 耗时{}s, 超时时间{}s", selector, consumerSecond, timeout);
                }
                ThreadUtils.sleep(1000);
            } else {
                break;
            }
        }
        int consumerSecond = TimeUtils.getSecondDiff(start);
        log.debug("未找到子元素{}, 耗时{}s", selector, consumerSecond);
        return null;
    }

    public ListExt<DomElement> $$(String selector) {
        return $$(selector, 0);
    }
    public ListExt<DomElement> $$(String selector, int timeout) {
        int start = TimeUtils.getCurSecond();
        for(int i = 0; i == 0 || i < timeout; i ++) {
            ListExt<DomElement> domElementList = DomElement.$$(multipleBrowser, page, frame, selector, timeout);
            int consumerSecond = TimeUtils.getSecondDiff(start);
            if(CollectionUtils.isEmpty(domElementList)) {
                if(consumerSecond < timeout) {
                    log.debug("等待子元素{}， 耗时{}s, 超时时间{}s", selector, consumerSecond, timeout);
                    ThreadUtils.sleep(1000);
                } else {
                    break;
                }
            } else {
                log.debug("找到子元素{}， 耗时{}s, 超时时间{}s", selector, consumerSecond, timeout);
                return domElementList;
            }
        }

        int consumerSecond = TimeUtils.getSecondDiff(start);
        log.debug("未找到子元素{}, 耗时{}s", selector, consumerSecond);
        return ListExt.emptyList();
    }

    @Override
    public ListExt<DomElement> $$(String selector, String... replaces) {
        if(replaces == null || replaces.length == 0) {
            replaces = nonReplaces;
        }

        for(String replace: replaces) {
            String curSelector = selector.replace("{}", replace);
            ListExt<DomElement> domElements = this.$$(curSelector);
            if(!CollectionUtils.isEmpty(domElements)) {
                return domElements;
            }
        }
        return ListExt.emptyList();
    }

    public String attr(String attrName) {
        return (String) evaluate("el => el.getAttribute('" + attrName + "')");
    }
    public String url() {
    	return frame.url();
    }
    public void attr(String attrName, String attrValue) {
        evaluate(String.format("el => el.setAttribute('%s', '%s')", attrName, attrValue));
    }


    public Object evaluate(String pageFunction, Object... args) {
        return frame.evaluate(pageFunction, args == null || args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
    }
    public JSHandle evaluateHandle(String pageFunction, Object... args) {
        return frame.evaluateHandle(pageFunction, args == null || args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
    }
}
