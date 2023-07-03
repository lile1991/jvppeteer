package com.ruiyun.jvppeteer.dom;

import com.ruiyun.jvppeteer.core.browser.MultipleBrowser;
import com.ruiyun.jvppeteer.core.page.ElementHandle;
import com.ruiyun.jvppeteer.core.page.Frame;
import com.ruiyun.jvppeteer.core.page.JSHandle;
import com.ruiyun.jvppeteer.core.page.Page;
import com.ruiyun.jvppeteer.enums.KeyCodeEnum;
import com.ruiyun.jvppeteer.exception.ProtocolException;
import com.ruiyun.jvppeteer.options.ClickOptions;
import com.ruiyun.jvppeteer.options.Clip;
import com.ruiyun.jvppeteer.protocol.input.ClickablePoint;
import com.ruiyun.jvppeteer.protocol.runtime.RemoteObject;
import com.ruiyun.jvppeteer.util.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * DOM元素封装
 */
@Slf4j
public class DomElement extends IDomFind {
    public static final String RN_ID = "rn-id";

    private final MultipleBrowser multipleBrowser;
    public final String rnId;
    public final String selector;
    public final Page page;
    @Setter
    public Frame frame;
    public ElementHandle elementHandle;

    public DomElement(MultipleBrowser multipleBrowser, Page page, ElementHandle elementHandle, String selector) {
       this(multipleBrowser, page, null, elementHandle, selector);
    }
    public DomElement(MultipleBrowser multipleBrowser, Page page, Frame frame, ElementHandle elementHandle, String selector) {
        this.multipleBrowser = multipleBrowser;
        this.page = page;
        this.frame = frame;
        this.elementHandle = elementHandle;

        // 选中的元素都赋值一个rnId
        this.rnId = randomRnId();
        attr(RN_ID, rnId);
        this.selector = selector == null ? "[" + RN_ID + "='" + rnId + "']" : selector;
    }

    public static String randomRnId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public ListExt<DomElement> $$(String selector) {
        multipleBrowser.checkBrowserStatus();
        return $$(selector, 0);
    }
    public ListExt<DomElement> $$(String selector, int timeout) {
        multipleBrowser.checkBrowserStatus();
        return $$(selector, timeout, null);
    }
    public ListExt<DomElement> $$(String selector, int timeout, Consumer<DomElement> flushFun) {
        multipleBrowser.checkBrowserStatus();
        int start = TimeUtils.getCurSecond();
        for(int i = 0; i == 0 || i < timeout; i ++) {
            List<ElementHandle> elementHandles = this.elementHandle.$$(selector);
            int consumerSecond = TimeUtils.getSecondDiff(start);
            if(CollectionUtils.isEmpty(elementHandles)) {
                if(consumerSecond < timeout) {
                    log.info("等待子元素{}， 耗时{}s, 超时时间{}s", selector, consumerSecond, timeout);
                    ThreadUtils.sleep(1000);
                    if(flushFun == null) {
                        flush();
                    } else {
                        flushFun.accept(this);
                    }
                } else {
                    break;
                }
            } else {
                log.debug("找到子元素{}， 耗时{}s, 超时时间{}s", selector, consumerSecond, timeout);
                return new ListExt<>(elementHandles.stream().map(ele -> new DomElement(multipleBrowser, page, frame, ele, null)).collect(Collectors.toList()));
            }
        }

        int consumerSecond = TimeUtils.getSecondDiff(start);
        log.debug("未找到子元素{}, 耗时{}s", selector, consumerSecond);
        return ListExt.emptyList();
    }

    /**
     * 刷新元素
     */
    public void flush() {
        multipleBrowser.checkBrowserStatus();
        ElementHandle elementHandle;
        if(frame != null) {
            elementHandle = frame.$("[" + RN_ID + "='" + rnId + "']");
            if(elementHandle == null) {
                elementHandle = frame.$(selector);
            }
        } else {
            elementHandle = page.$("[" + RN_ID + "='" + rnId + "']");
            if (elementHandle == null) {
                elementHandle = page.$(selector);
            }
        }

        if(elementHandle == null) {
            log.warn("找不到元素， 无法刷新");
        } else {
            this.elementHandle = elementHandle;
        }
    }
    public void flush(DomElement domElement) {
        multipleBrowser.checkBrowserStatus();
        if(domElement != null && domElement.elementHandle != null) {
            this.elementHandle = domElement.elementHandle;
        }
    }

    public DomElement $(String selector, String... replaces) {
        multipleBrowser.checkBrowserStatus();
        if(replaces == null || replaces.length == 0) {
            replaces = nonReplaces;
        }

        for(String replace: replaces) {
            String curSelector = selector.replace("{}", replace);
            DomElement domElement = this.$(curSelector);
            if(domElement != null) {
                return domElement;
            }
        }
        return null;
    }
    public ListExt<DomElement> $$(String selector, String... replaces) {
        multipleBrowser.checkBrowserStatus();
        if(replaces == null || replaces.length == 0) {
            replaces = nonReplaces;
        }

        for(String replace: replaces) {
            String curSelector = selector.replace("{}", replace);
            ListExt<DomElement> domElementList = this.$$(curSelector);
            if(!domElementList.isEmpty()) {
                return domElementList;
            }
        }
        return ListExt.emptyList();
    }

    public ListExt<DomElement> jQueries(String selector, int timeout, String... replaces) {
        multipleBrowser.checkBrowserStatus();
        ListExt<DomElement> domElements = super.jQueries(selector, timeout, replaces);
        //flush(rnId);
        return domElements;
    }

    @Override
    public DomElement $(String selector) {
        multipleBrowser.checkBrowserStatus();
        return $(selector, 0);
    }

    public DomElement $(String selector, int timeout) {
        multipleBrowser.checkBrowserStatus();
        return $(selector, timeout, (Consumer<DomElement>) null);
    }
    public DomElement $(String selector, int timeout, Consumer<DomElement> flushFun) {
        multipleBrowser.checkBrowserStatus();
        int start = TimeUtils.getCurSecond();
        for(int i = 0; i == 0 || i < timeout; i ++) {
            ElementHandle elementHandle = this.elementHandle.$(selector);
            int consumerSecond = TimeUtils.getSecondDiff(start);
            if(elementHandle != null) {
                log.debug("找到子元素{}， 耗时{}s, 超时时间{}s", selector, consumerSecond, timeout);
                return new DomElement(multipleBrowser, page, frame, elementHandle, selector);
            }

            if(consumerSecond < timeout) {
                log.debug("等待子元素{}， 耗时{}s, 超时时间{}s", selector, consumerSecond, timeout);
                ThreadUtils.sleep(1000);
                if(flushFun == null) {
                    flush();
                } else {
                    flushFun.accept(this);
                }
                if(this.elementHandle == null) {
                    break;
                }
            } else {
                break;
            }
        }
        int consumerSecond = TimeUtils.getSecondDiff(start);
        log.debug("未找到子元素{}, 耗时{}s", selector, consumerSecond);
        return null;
    }

    public static DomElement $(MultipleBrowser multipleBrowser, Page curPage, String selector, int timeout) {
        //multipleBrowser.checkBrowserStatus();
        return $(multipleBrowser, curPage, null, selector, timeout);
    }

    public static DomElement $(MultipleBrowser multipleBrowser, Page curPage, Frame frame, String oriSelector, int timeout) {
        multipleBrowser.checkBrowserStatus();
        String selector = oriSelector.replaceAll("'", "\\'");
        int start = TimeUtils.getCurSecond();
        ElementHandle elementHandle = null;
        ProtocolException protocolException = null;
        if(timeout > 0) {
            for(int i = 1; i <= timeout; i ++) {
                try {
                    log.debug("等待元素: {}, 第{}次", selector, i);
                    elementHandle = frame == null ? curPage.$(selector) : frame.$(selector);
                    if(elementHandle != null) {
                        break;
                    }
                } catch (Exception e) {
                    if(!multipleBrowser.isIgnoreProtocolException() && e instanceof ProtocolException) {
                        protocolException = (ProtocolException) e;
                    }
                    log.info("等待元素\"{}\"异常: {}， 继续等", selector, e.getMessage());
                }
                ThreadUtils.sleep(1000);
            }
        } else {
            try {
                elementHandle = frame == null ? curPage.$(selector) : frame.$(selector);
            } catch (Exception e) {
                log.warn("取元素【{}】异常， 再试一次: {}", selector, e.getMessage());

                try {
                    elementHandle = frame == null ? curPage.$(selector) : frame.$(selector);
                } catch (Exception e2) {
                    if(!multipleBrowser.isIgnoreProtocolException() && e2 instanceof ProtocolException) {
                        protocolException = (ProtocolException) e2;
                    }
                    log.error("第二次取元素【" + selector + "】依然异常", e);
                }
            }
        }
        if(elementHandle == null) {
            if(protocolException != null) {
                log.error("取元素发生协议异常: " + selector + "！ 共计耗时: " + TimeUtils.getSecondDiff(start) + "s, {}", protocolException.getMessage());
                throw protocolException;
            }
            log.debug("未取到元素: {}！ 共计耗时: {}s", selector, TimeUtils.getSecondDiff(start));
            return null;
        }
        log.debug("取到元素: {} 共计耗时: {}s, 超时时间: {}s", selector, TimeUtils.getSecondDiff(start), timeout);
        return new DomElement(multipleBrowser, curPage, frame, elementHandle, oriSelector);
    }


    public static ListExt<DomElement> $$(MultipleBrowser multipleBrowser, Page curPage, String selector, int timeout) {
        multipleBrowser.checkBrowserStatus();
        return $$(multipleBrowser, curPage, null, selector, timeout);
    }

    public static ListExt<DomElement> $$(MultipleBrowser multipleBrowser, Page curPage, Frame frame, String oriSelector, int timeout) {
        multipleBrowser.checkBrowserStatus();
        String selector = oriSelector.replaceAll("'", "\\'");
        int start = TimeUtils.getCurSecond();
        List<ElementHandle> elementHandles = null;
        ProtocolException protocolException = null;
        if(timeout > 0) {
            for(int i = 1; i <= timeout; i ++) {
                try {
                    log.debug("等待元素: {}, 第{}次", selector, i);
                    elementHandles = frame == null ? curPage.$$(selector) : frame.$$(selector);
                    if(elementHandles != null) {
                        break;
                    }
                } catch (Exception e) {
                    if(!multipleBrowser.isIgnoreProtocolException() && e instanceof ProtocolException) {
                        protocolException = (ProtocolException) e;
                    }
                    log.warn("等待元素\"{}\"异常: {}， 继续等", selector, e.getMessage());
                }
                ThreadUtils.sleep(1000);
            }
        } else {
            try {
                elementHandles = frame == null ? curPage.$$(selector) : frame.$$(selector);
            } catch (Exception e) {
                log.error("取元素【{}】异常, 再试一次: {}", selector, e.getMessage());

                try {
                    elementHandles = frame == null ? curPage.$$(selector) : frame.$$(selector);
                } catch (Exception e2) {
                    if(!multipleBrowser.isIgnoreProtocolException() && e2 instanceof ProtocolException) {
                        protocolException = (ProtocolException) e2;
                    }
                    log.error("第二次取元素【" + selector + "】依然异常", e);
                }
            }
        }
        if(elementHandles == null) {
            if(protocolException != null) {
                log.error("取元素发生协议异常: " + selector + "！ 共计耗时: " + TimeUtils.getSecondDiff(start) + "s, {}", protocolException.getMessage());
                throw protocolException;
            }
            log.warn("未取到元素: {}！ 共计耗时: {}s", selector, TimeUtils.getSecondDiff(start));
            return null;
        }
        log.debug("取到元素: {} 共计耗时: {}s, 超时时间: {}s", selector, TimeUtils.getSecondDiff(start), timeout);
        return new ListExt<>(elementHandles.stream().map(elementHandle -> new DomElement(multipleBrowser, curPage, frame, elementHandle, oriSelector)).collect(Collectors.toList()));
    }


    public String text() {
        multipleBrowser.checkBrowserStatus();
        try {
            if(frame != null) {
                return (String) frame.evaluate("el => el.innerText", Collections.singletonList(elementHandle));
            }
            return (String) page.evaluate("el => el.innerText", Collections.singletonList(elementHandle));
        } catch (Exception e) {
            log.error("获取元素文本异常", e);
            return "";
        }
    }
    public String text(String text) {
        multipleBrowser.checkBrowserStatus();
        try {
            if(frame != null) {
                return (String) frame.evaluate("el => el.innerText = '" + text + "'", Collections.singletonList(elementHandle));
            }
            return (String) page.evaluate("el => el.innerText = '" + text + "'", Collections.singletonList(elementHandle));
        } catch (Exception e) {
            log.error("获取元素文本异常", e);
            return "";
        }
    }
    public String outerText() {
        multipleBrowser.checkBrowserStatus();
        try {
            if(frame != null) {
                return (String) frame.evaluate("el => el.outerText", Collections.singletonList(elementHandle));
            }
            return (String) page.evaluate("el => el.outerText", Collections.singletonList(elementHandle));
        } catch (Exception e) {
            log.error("获取元素文本异常", e);
            return "";
        }
    }

    public String outerHTML() {
        multipleBrowser.checkBrowserStatus();
        try {
            if(frame != null) {
                return (String) frame.evaluate("el => el.outerHTML", Collections.singletonList(elementHandle));
            }
            return (String) page.evaluate("el => el.outerHTML", Collections.singletonList(elementHandle));
        } catch (Exception e) {
            log.error("获取元素文本异常", e);
            return "";
        }
    }

    public String html() {
        multipleBrowser.checkBrowserStatus();
        try {
            if(frame != null) {
                return (String) frame.evaluate("el => el.innerHTML", Collections.singletonList(elementHandle));
            }
            return (String) page.evaluate("el => el.innerHTML", Collections.singletonList(elementHandle));
        } catch (Exception e) {
            log.error("获取元素文本异常", e);
            return "";
        }
    }

    public String val() {
        multipleBrowser.checkBrowserStatus();
        try {
            if(frame != null) {
                return (String) frame.evaluate("el => el.value", Collections.singletonList(elementHandle));
            }
            return (String) page.evaluate("el => el.value", Collections.singletonList(elementHandle));
        } catch (Exception e) {
            log.error("获取元素文本异常", e);
            return "";
        }
    }

    public String val(Object value) {
        multipleBrowser.checkBrowserStatus();
        try {
            if(frame != null) {
                return (String) frame.evaluate("el => el.value = '" + value + "'", Collections.singletonList(elementHandle));
            }
            return (String) page.evaluate("el => el.value = '" + value + "'", Collections.singletonList(elementHandle));
        } catch (Exception e) {
            log.error("获取元素文本异常", e);
            return "";
        }
    }

    public boolean click() {
        //multipleBrowser.checkBrowserStatus();
        return click(0);
    }
    /**
     * 	点击后休眠指定秒
     * @param sleep 秒
     * @return
     */
    public boolean click(int sleep) {
        multipleBrowser.checkBrowserStatus();
        try {
            if(sleep > 1000) {
                log.error("按钮点击等待时间{}过长， 请注意单位是秒", sleep);
            }
            elementHandle.click();
            if(sleep > 0) {
                ThreadUtils.sleep(sleep * 1000);
            }
            return true;
        } catch (Exception e) {
            log.error("元素【" + toString() + "】click异常: {}", e.getMessage());
            return false;
        }
    }
    public boolean click(ClickOptions options, int offsetX, int offsetY) {
        //multipleBrowser.checkBrowserStatus();
        return click(options, true, offsetX, offsetY);
    }
    public boolean click(ClickOptions options, boolean isBlock, int offsetX, int offsetY) {
        multipleBrowser.checkBrowserStatus();
        log.debug("在元素上点击, 按钮位置{}, x轴偏移量: {}, y轴偏移量: {}", options.getButton(), offsetX, offsetY);
        ClickablePoint point;
        try {
            point = clickablePoint();
        } catch (Exception e) {
            log.info("坐标错误", e);
            return false;
        }
        if (!isBlock) {
            Helper.commonExecutor().submit(() -> {
                try {
                    this.page.mouse().click(point.getX() + offsetX, point.getY() + offsetY, options);
                } catch (InterruptedException e) {
                    log.info("点击鼠标异常", e);
                }
            });
            return true;
        } else {
            try {
                this.page.mouse().click(point.getX() + offsetX, point.getY() + offsetY, options);
                return true;
            } catch (Exception e) {
                log.info("点击鼠标异常", e);
                return false;
            }
        }
    }

    public ClickablePoint clickablePoint() {
        multipleBrowser.checkBrowserStatus();
        return elementHandle.clickablePoint();
    }

    @Override
    public String toString() {
        return selector == null ? elementHandle.toString() : selector;
    }

    public boolean clickRepeat(int timout, int clickParent) {
        //multipleBrowser.checkBrowserStatus();
        for(int i = 0; i < timout; i ++) {
            if(click() || clickParent(clickParent)) {
                return true;
            }

            if(i < timout - 1) {
                log.info("点击失败， 进行第{}次重试", i + 1);
                ThreadUtils.sleep(1000);
            }
        }
        log.info("点击失败， 重试次数{}已用完", timout);
        return false;
    }

    public boolean clickParent(int clickParent) {
        //multipleBrowser.checkBrowserStatus();
        if(clickParent > 0) {
            DomElement parent = parent();
            for(int p = 0; p < clickParent && parent != null; p ++, parent = parent.parent()) {
                if(parent.click()) {
                    log.info("点击上{}级父元素成功", p + 1);
                    return true;
                }
                log.info("点击上{}级父元素也失败", p + 1);
            }
        }
        return false;
    }

    public DomElement parent() {
        multipleBrowser.checkBrowserStatus();
        JSHandle jsHandle = evaluateHandle("el => el.parentElement");
        return jsHandle instanceof ElementHandle ? new DomElement(multipleBrowser, page, frame, (ElementHandle) jsHandle, null) : null;
    }
    public DomElement parents(String selector) {
        multipleBrowser.checkBrowserStatus();
        JSHandle jsHandle = evaluateHandle("el => jQuery(el).parents(\"" + selector + "\")[0]");
        return jsHandle instanceof ElementHandle ? new DomElement(multipleBrowser, page, frame, (ElementHandle) jsHandle, null) : null;
    }

    public void press(KeyCodeEnum... keys) {
        multipleBrowser.checkBrowserStatus();
        for(KeyCodeEnum key: keys) {
            press(key, 0, null);
        }
    }
    public void press(KeyCodeEnum key, int delay, String text) {
        multipleBrowser.checkBrowserStatus();
        try {
            elementHandle.press(key.code, delay, text);
        } catch (InterruptedException e) {
            log.info("发送按键【" + key + "】异常", e);
        }
    }
    public DomElement sendKeys(String text){
        //multipleBrowser.checkBrowserStatus();
        sendKeys(text, 0);
        return this;
    }
    public DomElement sendKeys(String text, int delay) {
        multipleBrowser.checkBrowserStatus();
        try {
            elementHandle.type(text, delay);
        } catch (Exception e) {
            log.info("发送字符【" + text + "】失败", e);
        }
        return this;
    }

    public String attr(String attrName) {
        //multipleBrowser.checkBrowserStatus();
        return (String) evaluate("el => el.getAttribute('" + attrName + "')");
    }
    public void attr(String attrName, String attrValue) {
        //multipleBrowser.checkBrowserStatus();
        evaluate(String.format("el => el.setAttribute('%s', '%s')", attrName, attrValue));
    }

    public Object evaluate(String pageFunction, Object... args) {
        multipleBrowser.checkBrowserStatus();
        try {
            return elementHandle.evaluate(pageFunction, args == null || args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
        } catch (Exception e) {
            log.error("元素执行evaluate异常", e);
            return null;
        }
    }
    public JSHandle evaluateHandle(String pageFunction, Object... args) {
        multipleBrowser.checkBrowserStatus();
        try {
            return (JSHandle) elementHandle.evaluateHandle(pageFunction, args == null || args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
        } catch (Exception e) {
            log.error("元素执行evaluate异常, pageFunction=" + pageFunction
                    + (args == null || args.length == 0 ? "" : ", args=" + Arrays.asList(args)), e);
            return null;
        }
    }


    public void scrollIntoViewIfNeeded() {
        multipleBrowser.checkBrowserStatus();
        try {
            elementHandle.scrollIntoViewIfNeeded();
            // curPage.evaluate("arguments[0].scrollIntoViewIfNeeded(true);", );
            // ThreadUtils.sleep(200);
        } catch (Exception e) {
            log.error("滚动到指定元素异常", e);
        }
    }

    /**
     * 获取元素样式值
     * @param name 样式名， 如： backgroundPosition
     * @return 样式值
     */
    public Object getComputedStyle(String name) {
        multipleBrowser.checkBrowserStatus();
        multipleBrowser.checkBrowserStatus();
        JSHandle jsHandle = evaluateHandle("el => window.getComputedStyle(el)." + name);
        RemoteObject remoteObject = jsHandle.getRemoteObject();
        return remoteObject == null ? null : remoteObject.getValue();
    }

    public static RemoteObject getRemoteObject(JSHandle jsHandle) {
        if(jsHandle == null) {
            return null;
        }

        return jsHandle.getRemoteObject();
    }

    public boolean isVisible() {
        multipleBrowser.checkBrowserStatus();
        /*
        参考: https://stackoverflow.com/questions/19669786/check-if-element-is-visible-in-dom
        function isVisible(elem) {
            if (!(elem instanceof Element)) throw Error('DomUtil: elem is not an element.');
            const style = getComputedStyle(elem);
            if (style.display === 'none') return false;
            if (style.visibility !== 'visible') return false;
            if (style.opacity < 0.1) return false;
            if (elem.offsetWidth + elem.offsetHeight + elem.getBoundingClientRect().height +
                elem.getBoundingClientRect().width === 0) {
                return false;
            }
            const elemCenter   = {
                x: elem.getBoundingClientRect().left + elem.offsetWidth / 2,
                y: elem.getBoundingClientRect().top + elem.offsetHeight / 2
            };
            if (elemCenter.x < 0) return false;
            if (elemCenter.x > (document.documentElement.clientWidth || window.innerWidth)) return false;
            if (elemCenter.y < 0) return false;
            if (elemCenter.y > (document.documentElement.clientHeight || window.innerHeight)) return false;
            let pointContainer = document.elementFromPoint(elemCenter.x, elemCenter.y);
            do {
                if (pointContainer === elem) return true;
            } while (pointContainer = pointContainer.parentNode);
            return false;
        }
         */
        Boolean isVisible = (Boolean) evaluate("elem => {" +
                "if (!(elem instanceof Element)) throw Error('DomUtil: elem is not an element.');\n" +
                "    const style = getComputedStyle(elem);\n" +
                "    if (style.display === 'none') return false;\n" +
                "    if (style.visibility !== 'visible') return false;\n" +
                "    if (style.opacity < 0.1) return false;\n" +
                "    if (elem.offsetWidth + elem.offsetHeight + elem.getBoundingClientRect().height +\n" +
                "        elem.getBoundingClientRect().width === 0) {\n" +
                "        return false;\n" +
                "    }\n" +
                "    const elemCenter   = {\n" +
                "        x: elem.getBoundingClientRect().left + elem.offsetWidth / 2,\n" +
                "        y: elem.getBoundingClientRect().top + elem.offsetHeight / 2\n" +
                "    };\n" +
                "    if (elemCenter.x < 0) return false;\n" +
                "    if (elemCenter.x > (document.documentElement.clientWidth || window.innerWidth)) return false;\n" +
                "    if (elemCenter.y < 0) return false;\n" +
                "    if (elemCenter.y > (document.documentElement.clientHeight || window.innerHeight)) return false;\n" +
                "    let pointContainer = document.elementFromPoint(elemCenter.x, elemCenter.y);\n" +
                "    do {\n" +
                "        if (pointContainer === elem) return true;\n" +
                "    } while (pointContainer = pointContainer.parentNode);\n" +
                "    return false" +
                "}");
        return isVisible != null && isVisible;
    }

    public boolean waitHide(int timeout) {
        //multipleBrowser.checkBrowserStatus();
        for(int i = 0; i < timeout; i ++) {
            if(! isVisible()) {
                log.debug("元素已隐藏");
                return true;
            }

            if(i % 5 == 0) {
                log.debug("等待元素隐藏");
            }
            ThreadUtils.sleep(1000);
        }
        return false;
    }


    public double x() {
        String width = (String) getComputedStyle("x");
        return pxToInt(width);
    }


    public double y() {
        String width = (String) getComputedStyle("y");
        return pxToInt(width);
    }

    public int width() {
        String width = (String) getComputedStyle("width");
        return pxToInt(width);
    }

    public int height() {
        String height = (String) getComputedStyle("height");
        return pxToInt(height);
    }

    private int pxToInt(String size) {
        try {
            int dIdx = size.indexOf(".");
            if(dIdx > 0) {
                size = size.substring(0, dIdx);
            }
            size = size.replaceAll("[^\\d]+", "");
            return Integer.parseInt(size);
        } catch (Exception e) {
            log.error("转换尺寸【" + size + "】异常", e);
            return 0;
        }
    }

    public void paste() {
        page.keyboard().down(KeyCodeEnum.Control.code, null);
        page.keyboard().down(KeyCodeEnum.V.code, null);
        page.keyboard().up(KeyCodeEnum.V.code);
        page.keyboard().up(KeyCodeEnum.Control.code);
    }

    public DomElement clearText() {
        for(int i = 0; i < 15; i ++) {
            press(KeyCodeEnum.Delete, KeyCodeEnum.Backspace, KeyCodeEnum.Delete, KeyCodeEnum.Backspace, KeyCodeEnum.Delete, KeyCodeEnum.Backspace);
        }
        return this;
    }


    public Clip boundingBox() {
        return this.elementHandle.boundingBox();
    }


    public static void waitFor(Supplier<Boolean> supplier, int timeout) {
        int start = TimeUtils.getCurSecond();
        Exception exception = new Exception();
        StackTraceElement[] stackTrace = exception.getStackTrace();
        StackTraceElement stackTraceElement = stackTrace[1];

        for(int i = 0; i < timeout; i ++) {
            int secondDiff = TimeUtils.getSecondDiff(start);
            if(supplier.get()) {
                log.debug("waitFor成功: {}, 耗时{}s, 超时时间{}s", supplier, secondDiff, timeout);
                return;
            }

            if(secondDiff > timeout) {
                log.error("waitFor【" + supplier + "】超时， 共计耗时 " + TimeUtils.getSecondDiff(start) + "s");
                return;
            }
            log.debug("waitFor: {}, 已耗时{}s, 超时时间{}s", stackTraceElement, secondDiff, timeout);
            ThreadUtils.sleep(1000);
        }
    }

    public void focus() {
        this.elementHandle.focus();
    }

    public void remove() {
        evaluate("el => el.remove()");
    }
}
