package com.ruiyun.jvppeteer.core.browser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruiyun.jvppeteer.core.Puppeteer;
import com.ruiyun.jvppeteer.core.page.Frame;
import com.ruiyun.jvppeteer.core.page.*;
import com.ruiyun.jvppeteer.dom.DomElement;
import com.ruiyun.jvppeteer.dom.DomFrame;
import com.ruiyun.jvppeteer.dom.IDomFind;
import com.ruiyun.jvppeteer.enums.BrowserEnum;
import com.ruiyun.jvppeteer.enums.BrowserLanguageEnum;
import com.ruiyun.jvppeteer.enums.DomMatchMode;
import com.ruiyun.jvppeteer.events.EventHandler;
import com.ruiyun.jvppeteer.events.Events;
import com.ruiyun.jvppeteer.exception.BrowserAlreadyClosedException;
import com.ruiyun.jvppeteer.exception.NavigateException;
import com.ruiyun.jvppeteer.options.*;
import com.ruiyun.jvppeteer.protocol.network.Cookie;
import com.ruiyun.jvppeteer.protocol.network.CookieParam;
import com.ruiyun.jvppeteer.protocol.network.DeleteCookiesParameters;
import com.ruiyun.jvppeteer.transport.Connection;
import com.ruiyun.jvppeteer.util.*;
import com.ruiyun.jvppeteer.util.os.OS;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.util.NamedThreadFactory;

import java.awt.*;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 多浏览器
 */
@Slf4j
public class MultipleBrowser extends IDomFind {
    /** 浏览器类型 */
    public final BrowserEnum browserEnum;
    /** jvppeteer驱动类 */
    @Setter
    public Browser browser;
    /** 启动时间 */
    @Getter
    private final Date launchTime;

    protected boolean isClosed = false;
    protected boolean isProcessExist = false;

    @Getter
    @Setter
    private boolean ignoreProtocolException = true;

    @Getter
    private final String id;

    /** 线程池 */
    private static final ExecutorService executorService = new ThreadPoolExecutor(45, 45,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new NamedThreadFactory("MultipleBrowser"));

    private Page curPage;

    /** 连接关闭事件处理器*/
    private final Map<String,Set<EventHandler<?>>> listenerMap = new ConcurrentHashMap<>();

    public void setBrowser(String id, Browser browser) {
        this.browser = browser;
    }

    /**
     * <p>监听浏览器事件disconnected</p>
     * <p>浏览器一共有四种事件</p>
     * <p>method ="disconnected","targetchanged","targetcreated","targetdestroyed"</p>
     *
     * @param handler 事件处理器
     */
    public void onDisconnected(EventHandler<Object> handler) {
        on(Events.BROWSER_DISCONNECTED.getName(), handler);
    }

    /**
     * 监听事件，可用于自定义事件监听,用户监听的事件都是在别的线程中异步执行的
     * @param method 事件名称
     * @param handler 事件的处理器
     */
    public void on(String method, EventHandler<?> handler) {
        Set<EventHandler<?>> listenerSet = listenerMap.get(method);
        if(listenerSet == null) {
            listenerSet = new CopyOnWriteArraySet<>();
        }
        listenerSet.add(handler);
        this.listenerMap.put(method, listenerSet);

        browser.on(method, handler);
    }

    public static Browser connect(String wsEndpoint, String browserURL, int timeout) {
        return connect(wsEndpoint, browserURL, null, timeout);
    }
    public static Browser connect(String wsEndpoint, String browserURL, Viewport viewport, int timeout) {
        LaunchOptionsBuilder optionsBuilder = new LaunchOptionsBuilder();

        // 一下设置参数有卵用， 浏览器已经打开了， 这里是进行connect， 不设置显示会不正常
        ArrayList<String> argList = new ArrayList<>();
        argList.add("--start-maximized");
//		argList.add("--no-sandbox");
        argList.add("--disable-gpu");
        argList.add("--disable-dev-shm-usage");
        // argList.add("-–disable-setuid-sandbox");
        argList.add("-–no-first-run");
        argList.add("-–no-zygote");
        argList.add("--disable-infobars");
        argList.add("-–single-process");//将Dom解析和渲染放到同一进程，能提升时间（进程上下文切换的时间）
        // argList.add("--lang=en-US");
        // argList.add("--disable-notifications");
        // argList.add("--test-type");
        argList.add("--ignore-certificate-errors");

        List<String> ignoreArgs = new ArrayList<>();
        ignoreArgs.add("--enable-automation");
        LaunchOptionsBuilder launchOptionsBuilder = optionsBuilder
                .withViewport(null)
                .withDumpio(true)
                .withArgs(argList)
                .withHeadless(false)
                .withIgnoreHTTPSErrors(true)
                .withIgnoreDefaultArgs(ignoreArgs);
        if(viewport != null) {
            launchOptionsBuilder.withViewport(viewport);
        }

        LaunchOptions options = launchOptionsBuilder.build();


        // Future<Browser> future = executorService.submit(() -> Puppeteer.connect(options, wsEndpoint, browserURL, null));
        // Browser browser = future.get(timeout, TimeUnit.SECONDS);
        // if(browser == null) {
        //     future.cancel(true);
        // }
        int curSecond = TimeUtils.getCurSecond();
        Browser browser = Puppeteer.connect(options, wsEndpoint, browserURL, null);
        log.debug("连接浏览器 {} 成功， 耗时{}s", ConvertUtils.defaultValue(wsEndpoint, browserURL), TimeUtils.getSecondDiff(curSecond));
        return browser;
    }


    /*public boolean processIsExists(String processQueryStr) {
        List<Integer> processIds = OS.command().findProcessIds(processQueryStr);
        return processIds != null && processIds.size() > 1;
    }*/

    public synchronized boolean reconnect(String processQueryStr) {
        if(isClosed) {
            log.debug("浏览器已关闭， 不尝试重连");
            return false;
        }
        log.debug("尝试重连");

        // 浏览器进程还在
        List<Integer> processIds = OS.command().findProcessIds(processQueryStr);
        for(int i = 0; i < 10 && !isClosed; i ++) {
            ThreadUtils.sleep(300);
            processIds = OS.command().findProcessIds(processQueryStr);

            if(processIds != null && processIds.size() > 1) {
                log.debug("浏览器进程还在.. 继续观察...{}", i + 1);
            } else {
                log.debug("浏览器进程 {} 不在了..{}, 直接重连失败", processQueryStr, i + 1);
                isProcessExist = true;
                return false;
            }
        }

        boolean isConnected = false;
        while(!isClosed && processIds != null && processIds.size() > 1) {
            log.debug("已与浏览器断开连接, 上下文却还没关闭，进程 {} 也还在 重连试试", processIds);
            if(reconnect(10)) {
                isConnected = true;
                break;
            }
            ThreadUtils.sleep(15 * 1000);
            processIds = OS.command().findProcessIds(processQueryStr);
        }

        if(!isConnected) {
            log.debug("浏览器重连失败， 进程只剩 {} ！ 关闭浏览器", processIds);
            close();
        }

        return isConnected && !isClosed;
    }

    public synchronized boolean reconnect(int timeout) {
        if(isConnected()) {
            log.debug("当前浏览器已经是Connected状态");
            return true;
        }

        if (isClosed) {
            log.debug("浏览器已关闭， 放弃重连");
            return false;
        }

        try {
            this.curPage = null;
            Browser browser = connect(this.browser.wsEndpoint(), null, timeout);
            Assert.notNull(browser, "重连浏览器超时");

            // 重新监听所有事件
            listenerMap.forEach((method, handlers) ->
                // 要用browser.on， 千万不要用this::on， 会导致事件重复绑定
                handlers.forEach(handler -> browser.on(method, handler))
            );
            // 重新赋值
            this.browser = browser;
            log.debug("浏览器重连成功");
            return true;
        } catch (Exception e) {
            log.error("重连浏览器失败", e);
            return false;
        }
    }

    public void reload() {
        checkBrowserStatus();
        PageNavigateOptions options = new PageNavigateOptions();
        options.setTimeout(100 * 1000);
        getCurPage().reload(options);
    }

    public void checkBrowserStatus() {
        if(isClosed) {
            throw new BrowserAlreadyClosedException("The browser has been closed");
        }
    }

    public void screenshot(String path) {
        if(!isClosed) {
            try {
                File file = new File(path);
                file.getParentFile().mkdirs();
                getCurPage().screenshot(path);
            } catch (Exception e) {
                log.error("截图异常", e);
            }
        }
    }

    public void savePage(String path) {
        if(!isClosed) {
            try {
                String html = html();
                if (html == null) {
                    log.warn("取页面内容失败");
                    return;
                }
                File file = new File(path);
                file.getParentFile().mkdirs();
                FileUtils.writeStringToFile(file, html, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("保存页面异常", e);
            }
        }
    }

    public String html() {
        return (String) evaluate("() => document.getElementsByTagName('html')[0].outerHTML");
    }

    public Page getCurPage() {
        checkBrowserStatus();
    	if(curPage == null || curPage.isClosed()) {
            List<Page> pages = null;
    	    for(int i = 0; i < 3; i ++) {
    	        pages = pages();
    	        if(CollectionUtils.isEmpty(pages)) {
    	            ThreadUtils.sleep(1000);
                } else {
    	            break;
                }
            }
    		if (CollectionUtils.isEmpty(pages)) {
                // curPage = newPage();
                log.warn("浏览器pages为空: {}", pages);
    		} else {
    			curPage = pages.get(0);
    		}
    	}
    	return curPage;
    }

    public List<Page> pages() {
        checkBrowserStatus();
        Future<List<Page>> future = executorService.submit(browser::pages);
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("调用pages方法超时", e);
            throw new RuntimeException("调用pages方法超时");
        }
    }

    public void goBack() {
        checkBrowserStatus();
        getCurPage().goBack();
    }

    public Page newPage() {
        checkBrowserStatus();
        curPage = newPage(null);
        return curPage;
    }
    public Page newPage(BrowserLanguageEnum browserLanguage) {
        checkBrowserStatus();
        try {
            curPage = browser.newPage();
        } catch (Exception e) {
            throw new RuntimeException("新建标签页异常", e);
        }
        curPage.setDefaultNavigationTimeout(3 * 60 * 1000);//设置3分钟超时时间

        // 设置页面语言
        if(browserLanguage != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept-Language", browserLanguage.browserLanguage + ",en;q=0.9");
            curPage.setExtraHTTPHeaders(headers);

            curPage.evaluateOnNewDocument("() =>  {\n" +
            "      Object.defineProperty(navigator, 'language', {\n" +
            "        get: function () {\n" +
            "          return '" + browserLanguage.browserLanguage + "'\n" +
            "        }\n" +
            "      })\n" +
            "      Object.defineProperty(navigator, 'languages', {\n" +
            "        get: function () {\n" +
            "          return ['" + browserLanguage.browserLanguage + "', '" + browserLanguage.browserLanguage.split("-")[0] + "']\n" +
            "        }\n" +
            "      })\n" +
            "    }");
        }
        /*page.onPageerror(dialog-> {
            log.debug("onPageerror{}", dialog);
        });
        page.onMetrics(dialog-> {
            log.debug("onMetrics{}", dialog);
        });
        page.onPopup(dialog-> {
            log.debug("onPopup{}", dialog);
        });
        page.onDialg(dialog -> {
            log.debug("onDialg{}", dialog);
            dialog.dismiss();
        });*/
        return curPage;
    }

    public List<DomFrame> frames() {
        checkBrowserStatus();
        return getCurPage().frames().stream().map(f -> new DomFrame(this, getCurPage(), f)).collect(Collectors.toList());
    }
    public List<DomFrame> frames(int timeout) {
        checkBrowserStatus();
    	for (int i = 0; i < timeout; i++) {
    		List<DomFrame> frames = frames();
    		if (frames != null && frames.size() > 0) {
				return frames;
			}
    		ThreadUtils.sleep(1000);
		}
    	return null;
    }

    public MultipleBrowser(String id, BrowserEnum browserEnum, Browser browser) {
        this.id = id;
        this.browserEnum = browserEnum;
        this.browser = browser;
        this.launchTime = new Date();
    }

    /**
     * 关闭浏览器
     */
    public synchronized int close() {
        if(isClosed) {
            log.debug("浏览器已关闭");
            return 0;
        }
        isClosed = true;

        int start = TimeUtils.getCurSecond();
        log.debug("开始关闭浏览器: {}", browser);
        // 关闭浏览器
        try {
            if(browser != null) {
                browser.close();
            }
            log.debug("关闭浏览器 {} 成功", browser);
        } catch (Exception e) {
            log.warn("关闭浏览器 " + browser + " 异常， 耗时" + TimeUtils.getSecondDiff(start) + "s, 请注意手动释放进程: {}", e.getMessage());
        }

        try {
            // 杀进程
            OS.command().killByQueryStr(id);
        } catch (Throwable ignore) {
        }
        return 1;
    }

    public boolean click(Supplier<DomElement> supplier, int timeout, int clickParent) {
        checkBrowserStatus();
        int start = TimeUtils.getCurSecond();
        for(int i = 0; i < timeout; i ++) {
            DomElement domElement = supplier.get();
            if(domElement == null) {
                log.debug("点击元素失败， 未取到元素");
                break;
            }

            if(domElement.click() || domElement.clickParent(clickParent)) {
                log.debug("点击元素成功");
                return true;
            } else {
                int consumerTime = TimeUtils.getSecondDiff(start);
                if(consumerTime < timeout * 1000) {
                    log.debug("点击元素异常， 重试, 已消耗时间: {}s, 超时时间{}s", consumerTime, timeout);
                    ThreadUtils.sleep(1000);
                } else {
                    log.debug("点击元素超时, 总耗时{}s", consumerTime);
                    break;
                }
            }
        }
        return false;
    }
    public DomElement $(String selector) {
        checkBrowserStatus();
        return $(selector, 0);
    }
    public DomElement $(String selector, int timeout) {
        checkBrowserStatus();
        return $(getCurPage(), selector, timeout);
    }
    public DomElement $(Page page, String selector, int timeout) {
        checkBrowserStatus();
        return DomElement.$(this, page, selector, timeout);
    }

    public ListExt<DomElement> $$(String selector) {
        checkBrowserStatus();
        return $$(selector, 0);
    }
    public ListExt<DomElement> $$(String selector, int timeout) {
        checkBrowserStatus();
        Page curPage = getCurPage();
        if(timeout > 0) {
            DomElement.waitFor(() -> !CollectionUtils.isEmpty(curPage.$$(selector)), timeout);
        }
        return DomElement.$$(this, curPage, null, selector, timeout);
    }
    public ListExt<DomElement> $$(String selector, String... replaces) {
        checkBrowserStatus();
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

    /**
     * 打开空白页面
     */
    public Response openInNewTab(String url) throws NavigateException {
        checkBrowserStatus();
        return openInNewTab(url, 100);
    }
    public Response openInNewTab(String url, int timeoutSecond) throws NavigateException {
        checkBrowserStatus();
        try {
            curPage = newPage();
            if(StringUtils.isNotBlank(url)) {
                return goTo(url, timeoutSecond);
            }
            return null;
        } catch (NavigateException e) {
            log.debug("导航到【" + url + "】异常", e);
            throw e;
        } catch (Exception e) {
            log.debug("打开URL【" + url + "】异常", e);
            return null;
        }
    }

    public Response goTo(String url) {
        checkBrowserStatus();
        try {
            return goTo(url, 100);
        } catch (Exception e) {
            log.debug("打开URL【" + url + "】异常", e);
            // throw e;
            return null;
        }
    }

    public Response goToThrowException(String url) throws NavigateException {
        checkBrowserStatus();
        return goTo(url, 100);
    }

    public Response goTo(String url, int timeoutSecond)/* throws NavigateException, com.ruiyun.jvppeteer.exception.TimeoutException*/ {
        checkBrowserStatus();
        PageNavigateOptions pageNavigateOptions = new PageNavigateOptions();
        pageNavigateOptions.setTimeout(timeoutSecond * 1000);
        try {
            return getCurPage().goTo(url, pageNavigateOptions);
        } catch (InterruptedException e) {
            log.error("goTo url " + url + " InterruptedException", e);
            return null;
        }
    }

    /**
     * 显示或隐藏窗口
     * @return true-显示窗口,  false-隐藏窗口
     */
    public boolean toggleWindow() {
        boolean show;
        Bounds windowBounds = getWindowBounds(true);
        if (Bounds.minimized.equals(windowBounds.getWindowState())) {
            windowBounds.setWindowState(Bounds.normal);
            show = true;
        } else {
            windowBounds = new Bounds(Bounds.minimized);
            show = false;
        }
        setWindowBounds(windowBounds, false);
        return show;
    }

    public void deleteCookie() {
        checkBrowserStatus();
        List<Cookie> cookies = cookies();
        if(cookies == null || cookies.isEmpty()) {
            return;
        }
        List<DeleteCookiesParameters> deleteCookies = cookies.stream().map(cookieParam -> {
            DeleteCookiesParameters deleteCookiesParameters = new DeleteCookiesParameters();
            deleteCookiesParameters.setDomain(cookieParam.getDomain());
            deleteCookiesParameters.setName(cookieParam.getName());
            deleteCookiesParameters.setPath(cookieParam.getPath());
            return deleteCookiesParameters;
        }).collect(Collectors.toList());

        try {
            getCurPage().deleteCookie(deleteCookies);
        } catch (Exception e) {
            log.error("删除cookie异常", e);
        }
    }

    public String cookiesStr() {
        List<Cookie> cookies = cookies();
        return cookies.stream().map(cookie ->
            cookie.getName() + "=" + cookie.getValue()
        ).collect(Collectors.joining("; "));
    }
    public List<Cookie> cookies() {
        checkBrowserStatus();
        return getCurPage().cookies();
    }
    public void setCookie(List<CookieParam> cookies) {
        checkBrowserStatus();
        try {
            getCurPage().setCookie(cookies);
        } catch (Exception e) {
            log.error("设置cookie【" + cookies + "】异常", e);
        }
    }

    public void setLocalStorage(String localStoryKey, String toJsonString) {
        checkBrowserStatus();
        if(isClosed) {
            throw new IllegalStateException("浏览器已关闭， 无法执行脚本");
        }
        evaluate("localStorage.setItem('" + localStoryKey + "', '" + toJsonString + "')");
    }

    public boolean isConnected() {
        try {
            return browser.isConnected();
        } catch (Exception e) {
            log.error("获取浏览器连接状态异常", e);
            return false;
        }
    }

    public void switchToPage(Page page) {
        checkBrowserStatus();
        switchToPage(page, false);
    }

    /**
     * 切换到page标签， 并关闭当前标签
     * @param page 目标标签页
     * @param closeCurPage true-关闭当前标签， false-不关闭当前标签
     */
    public void switchToPage(Page page, boolean closeCurPage) {
        checkBrowserStatus();
        if(closeCurPage) {
            if(getCurPage() != page) {
                try {
                    getCurPage().close();
                } catch (InterruptedException e) {
                    log.error("关闭当前页签异常", e);
                }
                switchToPage(page);
            }
        }
        page.bringToFront();
        this.curPage = page;
    }
    public void switchToNewPage(List<Page> pages) {
        checkBrowserStatus();
        Optional<Page> first = browser.pages().stream().filter(p -> !pages.contains(p)).findFirst();
        first.ifPresent(this::switchToPage);
    }


    /**
     * 滚到末尾
     * @return -1 没有下一页， 0/-2-滚动失败/页面蹦了，刷新页面  1-滚动成功
     */
    public int scrollIntoLast() {
        Integer offsetHeightBefore = null;
        for(int i = 0, curScone = TimeUtils.getCurSecond(); TimeUtils.getCurSecond() - curScone < 6; i ++) {
            log.debug("开始做下拉动作 scrollIntoLast");
            offsetHeightBefore = (Integer) evaluate("document.body.offsetHeight");
            log.debug("当前页面高度: {}", offsetHeightBefore);
            if (offsetHeightBefore == null) {
                if(i < 3) {
                    log.warn("第{}次取页面高度失败， 重试", i + 1);
                    continue;
                }
            }
            break;
        }
        if(offsetHeightBefore == null) {
            log.error("没有取到页面高度， 返回0, reload页面");
            return -2;
        }

        return isOffsetHeightChange(offsetHeightBefore);
    }

    private int isOffsetHeightChange(Integer offsetHeightBefore) {
        for (int i = 0, curSecond = TimeUtils.getCurSecond(); i < 10 && TimeUtils.getCurSecond() - curSecond < 15; i++) {
            if (i % 3 == 0) {
                log.debug("第 {} 次拉到底", i);
            }
            evaluate("window.scrollTo(0, document.body.offsetHeight)");
            ThreadUtils.sleep(1000);
            // 等待下拉框变化
            Object offsetHeight = evaluate("document.body.offsetHeight");
            if (offsetHeight == null) {
                log.debug("获取页面高度为空, 可能页面已经Crash");
                return -2;
            }
            Integer offsetHeightAfter = (Integer) offsetHeight;
            if (offsetHeightAfter > offsetHeightBefore) {
                log.debug("页面高度由{} 变为 {}， 拿数据去咯， 果果嗒！！", offsetHeightBefore, offsetHeightAfter);
                return 1;
            }
            log.debug("页面高度没有变化, 下拉前高度: {}, 当前高度: {}， 继续等 {}", offsetHeightBefore, offsetHeight, i + 1);
        }
        return 0;
    }

    /**
     * 获取当前页面URL
     * @return URL地址
     */
    public String url() {
        checkBrowserStatus();
        try {
            return getCurPage().target().url();
        } catch (Exception e) {
            log.error("取浏览器地址栏异常", e);
            return "";
        }
    }

    public void waitForward(String beforeUrl, int timeout) {
        checkBrowserStatus();
        int curSecond = TimeUtils.getCurSecond();
        for(int i = 0; i < timeout; i ++) {
            log.debug("等待页面\"{}\"重定向...", beforeUrl);
            String afterUrl = url();
            if(! afterUrl.equals(beforeUrl)) {
                log.debug("页面重定向成功， 新的URL地址: {}, 耗时{}s", afterUrl, TimeUtils.getSecondDiff(curSecond));
                return ;
            }
            ThreadUtils.sleep(1000);
        }
        log.warn("等待页面\"{}\"重定向超时！, 耗时{}s", beforeUrl, TimeUtils.getSecondDiff(curSecond));
    }

    /**
     * 登台文件选择框
     * @param timeout 超时时间，秒
     */
    public Future<FileChooser> waitForFileChooser(int timeout) {
         return getCurPage().waitForFileChooser(timeout * 1000);
    }
    public boolean chooseFile(Future<FileChooser> future, String... filePaths) {
        try {
            FileChooser fileChooser = future.get();
            fileChooser.accept(Arrays.asList(filePaths));
            return true;
        } catch (Exception e) {
            log.debug("选择文件失败");
            return false;
        }
    }

    /**
     * 执行Js
     * @param pageFunction 函数名
     */
    public Object evaluate(String pageFunction, Object... args) {
        checkBrowserStatus();
        try {
            return getCurPage().evaluate(pageFunction, args == null || args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
        } catch (Exception e) {
            log.error("页面执行evaluate异常, pageFunction=" + pageFunction
                    + (args == null || args.length == 0 ? "" : ", args=" + Arrays.asList(args)), e);
            return null;
        }
    }
    /**
     * 执行Js
     * @param pageFunction 函数名
     */
    public Object evaluateThrow(String pageFunction, Object... args) {
        checkBrowserStatus();
        return getCurPage().evaluate(pageFunction, args == null || args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
    }

    /**
     * 执行Js
     * 此方法和 evaluate 的唯一区别是此方法返回的是页内类型(JSHandle)
     * @param pageFunction 函数名
     */
    public JSHandle evaluateHandle(String pageFunction) {
        checkBrowserStatus();
        try {
            return getCurPage().evaluateHandle(pageFunction);
        } catch (Exception e) {
            log.error("页面执行evaluateHandle异常, pageFunction=" + pageFunction, e);
            return null;
        }
    }

    /**
     * 获取cookie值
     * @param name cookie名
     * @return 值
     */
    public String getCookieValue(String name) {
    	return getCurPage().cookies().stream().filter(cookie -> name.equalsIgnoreCase(cookie.getName())).findFirst().map(Cookie::getValue).orElse(null);
    }

    public void move(DomElement domElement) {
        checkBrowserStatus();
        Clip clip = domElement.boundingBox();
        move(clip.getX(), clip.getY());
    }

    public void move(double x, double y) {
        checkBrowserStatus();
        try {
            getCurPage().mouse().move(x, y);
        } catch (Exception e) {
            log.error("移动鼠标异常", e);
        }
    }
    /*public void move(double sx, double sy, double ex, double ey) {
        checkBrowserStatus();
        try {
            move(sx, sy);

            int count = RandomUtils.nextInt(10, 20);

            for(int i = 1; i < count; i ++) {
                move(sx + RandomUtils.nextInt(-800, 800), sy +  RandomUtils.nextInt(-900, 900));
            }

            move(ex, ey);
        } catch (Exception e) {
            log.error("移动鼠标异常", e);
        }
    }*/

    public void resizeTo(int width, int height) {
        evaluate("window.resizeTo(" + width + ", " + height + ");");
    }
    public void resizeBy(int offWidth, int offHeight) {
        evaluate("window.resizeBy(" + offWidth + ", " + offHeight + ");");
    }
    public void moveBy(int offX, int offY) {
        evaluate("window.moveBy(" + offX + ", " + offY + ");");
    }
    public void moveTo(int x, int y) {
        evaluate("window.moveTo(" + x + ", " + y + ");");
    }
    public void moveToCenter() {
        evaluate("window.moveTo((screen.availWidth - window.innerWidth) / 2, (screen.availHeight - window.innerHeight) / 2)");
    }

    /**
     * 获取Frame
     * @param name iframe的ID
     */
    public DomFrame frame(String name, int timeout) {
        checkBrowserStatus();
        return frame(f -> name.equals(f.getName()), timeout);
    }

    /**
     * 获取Frame
     * @param selectorFun 选择器函数
     */
    public DomFrame frame(Function<Frame, Boolean> selectorFun, int timeout) {
        checkBrowserStatus();
        for(int i = 0; i < timeout; i ++) {
            List<Frame> frames = getCurPage().frames();
            Optional<Frame> first = frames.stream().filter(selectorFun::apply).findFirst();
            if(first.isPresent()) {
                return new DomFrame(this, getCurPage(), first.get());
            }
            log.debug("等待iframe");
            ThreadUtils.sleep(1000);
        }
        return null;
    }

    public void setWindowBounds(Bounds bounds, boolean isWait) {
        setWindowBounds(getWindowId(), bounds, isWait);
    }
    public void setWindowBounds(int windowId, Bounds bounds, boolean isWait) {
        checkBrowserStatus();
        try {
            Connection connection = browser.target().browserContext().getConnection();
            Map<String, Object> params = new HashMap<>();
            params.put("windowId", windowId);
            params.put("bounds", bounds);
            JsonNode jsonNode = connection.send("Browser.setWindowBounds", params, isWait);
            log.debug("WS调整窗口{}大小为{}: {}", windowId, bounds, jsonNode);
        } catch (HeadlessException e) {
            log.error("修改窗口大小异常", e);
        }
    }

    public Integer getWindowId() {
        Target adsAccountTarget = browser.targets().stream().filter(t -> "page".equals(t.getTargetInfo().getType())).findFirst().orElse(null);
        if(adsAccountTarget != null) {
            Map<String, Object> params = new HashMap<>();
            params.put("targetId", adsAccountTarget.getTargetId());
            JsonNode jsonNode = browser.target().browserContext().getConnection().send("Browser.getWindowForTarget", params, true);
            String windowId = jsonNode.get("windowId").asText();
            log.debug("WS获取窗口ID{}成功: {}", params, windowId);
            return ConvertUtils.toInteger(windowId);
        }
        return null;
    }

    /**
     *
     * @param isWait 暂时只支持true
     */
    public Bounds getWindowBounds(boolean isWait) {
        return getWindowBounds(getWindowId(), isWait);
    }
    public Bounds getWindowBounds(int windowId, boolean isWait) {
        checkBrowserStatus();
        try {
            Connection connection = browser.target().browserContext().getConnection();
            Map<String, Object> params = new HashMap<>();
            params.put("windowId", getWindowId());
            JsonNode json = connection.send("Browser.getWindowBounds", params, isWait);
            log.debug("WS获取窗口{}大小成功: {}", windowId, json);

            JsonNode bounds = json.get("bounds");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(bounds.toString(), Bounds.class);
        } catch (Exception e) {
            log.error("获取窗口大小异常", e);
        }
        return null;
    }

    public void disconnect() {
        browser.disconnect();
    }

    public <T> Future<T> submitCallable(Callable<T> callable) {
        return executorService.submit(callable);
    }

    public void reloadToNewTab() {
        String url = url();
        Page curPage = getCurPage();

        log.debug("在新的标签页打开: {}", url);
        openInNewTab(url);

        try {
            log.debug("关闭上一个标签页");
            curPage.close();
        } catch (InterruptedException ignore) {
        }
    }

    public String userAgent() {
        checkBrowserStatus();
        return UserAgentUtils.trimUA(browser.userAgent());
    }

    public String checkNetworkErrorCode() {
        if($("head") == null
                || ($("#main-frame-error #main-content #main-message") != null && $("#sub-frame-error") != null)) {
            DomElement mainMessage = $("#main-message");
            DomElement errCodeEle = $("#main-message .error-code");
            String errorMsg = null;
            if(errCodeEle != null) {
                errorMsg = errCodeEle.text();
            }

            if(StringUtils.isBlank(errorMsg) && mainMessage != null) {
                DomElement h1 = mainMessage.$("h1");
                errorMsg = h1 == null ? null : h1.text();
            }

            if(errorMsg != null) {
                log.error("Network error [{}], at {}", errorMsg, url());
            }
            return errorMsg;
        }
        return null;
    }

    public Page page(String targetId) {
        List<Page> pages = pages();
        return pages.stream().filter(p -> p.target().getTargetId().equals(targetId)).findAny().orElse(null);
    }

    public boolean isVisibleBySelectors(DomMatchMode domMatchMode, String... selectors) {
        for(String select: selectors) {
            DomElement domElement = $(select);
            if(domElement != null && domElement.isVisible()) {
                if(domMatchMode == DomMatchMode.ANY) {
                    return true;
                }
            } else {
                if(domMatchMode == DomMatchMode.ALL) {
                    return false;
                }
            }
        }
        return domMatchMode == DomMatchMode.ALL;
    }
}
