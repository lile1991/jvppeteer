package com.ruiyun.jvppeteer.util.os;

import com.ruiyun.jvppeteer.util.ConvertUtils;
import com.ruiyun.jvppeteer.util.IOUtils;
import com.ruiyun.jvppeteer.util.StringUtils;
import com.ruiyun.jvppeteer.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class Win32Cmd implements OSCommand {

    private static final String ENCODING = "GBK";


    @Override
    public String chromeAppName() {
        return "chrome.exe";
    }

    @Override
    public void killPID(Serializable pid) {
        if(pid == null) {
            return;
        }
        try {
            log.info("Kill 进程PID: " + pid);
            Process process = Runtime.getRuntime().exec("taskkill /f /PID " + pid);
            process.waitFor(15, TimeUnit.SECONDS);
            echo(process);
        } catch (Exception e) {
            log.error("杀掉进程PID" + pid + "异常", e);
        }
    }

    @Override
    public void killPIDs(Serializable pid) {
        if(pid == null) {
            return;
        }
        try {
            log.info("Kill 进程PIDs: " + pid);
            Process process = Runtime.getRuntime().exec("taskkill /f /t /PID " + pid);
            process.waitFor(15, TimeUnit.SECONDS);
            echo(process);
        } catch (Exception e) {
            log.error("杀掉进程PID" + pid + "异常", e);
        }
    }

    /**
     * 设置剪切板文本内容
     * @param content 内容
     */
    public void setClipboard(String content){
        if (content == null) {
            content = "";
        }
        String vc = content.trim();
        StringSelection ss = new StringSelection(vc);
        Clipboard sysClb = Toolkit.getDefaultToolkit().getSystemClipboard();
        sysClb.setContents(ss,null);
    }
    /**
     * Get剪切板文本内容
     */
    public String getClipboardText(){
        // 获取系统剪贴板
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // 获取剪贴板中的内容
        Transferable trans = clipboard.getContents(null);
        if (trans != null) {
            // 判断剪贴板中的内容是否支持文本
            if (trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    // 获取剪贴板中的文本内容
                    String text = (String) trans.getTransferData(DataFlavor.stringFlavor);
                    return text;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 检查程序是否已启动
     * @return
     * @throws IOException
     */
    @Override
    public boolean isStarted() throws IOException {
        // 写个Shell公用类
        Process process = Runtime.getRuntime().exec("tasklist /fi \"IMAGENAME  eq Multilogin.exe\"");
        /*
        映像名称                       PID 会话名              会话#       内存使用
        ========================= ======== ================ =========== ============
        multilogin.exe               16120 Console                    1    170,764 K
         */
        List<String> lines = IOUtils.readLines(process.getInputStream(), ENCODING);
        for (String line : lines) {
            if (line != null && line.toLowerCase().toLowerCase().contains("multilogin.exe")) {
                return true;
            }
        }

        process = Runtime.getRuntime().exec("wmic process where \"commandline like '%multilogin%'\" get caption,commandline");
        /*
         * chrome.exe   "C:\Users\lile1\AppData\Local\Google\Chrome\Application\chrome.exe" --type=utility --utility-sub-type=network.mojom.NetworkService --field-trial-handle=2128,13510903053334612573,12900789602228915585,131072 --lang=zh-CN --service-sandbox-type=network --enable-audio-service-sandbox --mojo-platform-channel-handle=2480 /prefetch:8
         */
        lines = IOUtils.readLines(process.getInputStream(), ENCODING);
        for (String line : lines) {
            if (line != null && line.toLowerCase().contains("multilogin.exe")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean releasePort(int port, String... ignorePIDs) throws IOException {
        boolean isRelease = false;
        List<String> ignorePIDList = Collections.emptyList();
        if(ignorePIDs != null && ignorePIDs.length > 0) {
            ignorePIDList = Arrays.asList(ignorePIDs);
        }
        Process process = Runtime.getRuntime().exec("netstat -ano");
        /*
          TCP    0.0.0.0:35000          0.0.0.0:0              LISTENING       16120
          TCP    [::]:35000             [::]:0                 LISTENING       16120
         */
        List<String> lines = IOUtils.readLines(process.getInputStream(), ENCODING);
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                try {
                    int ipIdex = line.indexOf("0.0.0.0:" + port);
                    if(ipIdex == -1) {
                        ipIdex = line.indexOf("127.0.0.1:" + port);
                    }

                    if(ipIdex == -1) {
                        continue;
                    }

                    log.info("发现端口【{}】占用信息: {}", port, line);
                    line = line.substring(ipIdex);
                    line = line.substring(line.indexOf(":") + 1);

                    String[] split = line.split("\\s+");
                    String occupyPort = split[0];
                    if(occupyPort.equals(String.valueOf(port))) {
                        String pid = split[split.length - 1];
                        log.info("找到占用端口进程PID: {}", pid);
                        if(ignorePIDList.contains(pid)) {
                            log.info("纳尼！ PID【{}】在忽略名单中: {}", pid, ignorePIDList);
                        } else {
                            killPIDs(pid);
                            isRelease = true;
                            log.info("成功释放端口【{}】", port);
                        }
                    } else {
                        log.info("哦豁！ 端口【{}】实际没被占用", port);
                    }
                } catch (Exception e) {
                    log.warn("释放端口【" + port + "】【" + line + "】异常", e);
                }
            }
        }
        return isRelease;
    }

    /**
     * 检测端口是否被占用
     * @param port 端口
     * @return 占用程序PID
     * @deprecated Runtime 不支持管道, 这个方式要不得
     */
    @Deprecated
    public String detectOccupyPort(int port) throws IOException {
        Process process = Runtime.getRuntime().exec("netstat -ano | findstr " + port);
        /*
          TCP    0.0.0.0:35000          0.0.0.0:0              LISTENING       16120
          TCP    [::]:35000             [::]:0                 LISTENING       16120
         */
        List<String> lines = IOUtils.readLines(process.getInputStream(), ENCODING);
        for (String line : lines) {
            if (line != null) {
                String[] split = line.split("\\s");
                try {
                    String occupyPort = split[1].substring(split[1].lastIndexOf(":") + 1);
                    if(String.valueOf(port).equals(occupyPort)) {
                        return split[4];
                    }
                } catch (Exception ignore) {
                }
                // String pid = "";
                // Runtime.getRuntime().exec("taskkill /PID " + pid);
                log.info("login ml successful...");
            }
        }
        return null;
    }

    /**
     * 杀死selenium浏览器--bug 可能会导致阻塞
     * @return
     */
    @Override
    public void killChromeDriver() {
        try {
            String cmd = "taskkill /f /t /im chromedriver.exe";
            log.info("kill chromedriver.exe进程: {}", cmd);
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor(15, TimeUnit.SECONDS);
            echo(process);
        } catch (Exception e) {
            log.warn("关闭chromedriver进程异常", e);
        }
    }

    public void echo(Process process) {
        // log.info("echo...");
        try {
            InputStream inputStream = process.getInputStream();
            if(inputStream.available() > 0) {
                log.info(IOUtils.toString(inputStream, ENCODING));
            }
            InputStream errorStream = process.getErrorStream();
            if(errorStream.available() > 0) {
                log.debug(IOUtils.toString(errorStream, ENCODING));
            }
        } catch (IOException e) {
            log.error("获取控制台输出异常", e);
        }
    }

    /**
     * 杀死chrome浏览器
     * @return
     */
    @Override
    public void killChrome() {
        try {
            String cmd = "taskkill /f /t /im chrome.exe";
            log.info("kill chrome.exe进程: {}", cmd);
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor(15, TimeUnit.SECONDS);
            echo(process);
        } catch (Exception e) {
            log.warn("关闭chrome进程异常", e);
        }
    }

    @Override
    public void killMultilogin() {
        try {
            String cmd = "taskkill /f /t /im multilogin.exe";
            Process process = Runtime.getRuntime().exec(cmd);
            log.info("kill AdsBrowser.exe进程: {}", cmd);
            process.waitFor(15, TimeUnit.SECONDS);
            echo(process);
        } catch (Exception e) {
            log.warn("关闭ml进程异常", e);
        }
    }

    @Override
    public void killByQueryStr(String queryStr) {
        Set<Integer> allProcessIds = new HashSet<>();
        for(int i = 0; i < 2; i ++) {
            try {
                List<Integer> processIds = findProcessIds(queryStr);
                if(processIds != null) {
                    allProcessIds.addAll(processIds);
                }

                log.info("Kill 【{}】所有关联线程: {}", queryStr, processIds);
                for (Integer processId : allProcessIds) {
                    killPIDs(processId);
                }
            } catch (Exception e) {
                log.warn("kill 进程 " + queryStr + " 异常", e);
            } finally {
                ThreadUtils.sleep(500);
            }
        }

        // killNotResponding(OS.command().chromeAppName());
    }

    private static void killNotResponding(String processName) {
        try {
            // taskkill.exe -f -fi "status eq not responding" /t /im chrome.exe
            Process process = Runtime.getRuntime().exec("taskkill.exe -f -fi \"status eq not responding\" /t /im " + processName);
            process.waitFor(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Kill 失去响应的进程异常", e);
        }
    }

    @Override
    public List<Integer> findProcessIds(String queryStr) {
        try {
            Process process = Runtime.getRuntime().exec("wmic process where \"commandline like '%" + queryStr + "%'\" get ProcessId");
            process.waitFor(15, TimeUnit.SECONDS);
            InputStream inputStream = process.getInputStream();
            if(inputStream.available() > 0) {
                String sids = IOUtils.toString(inputStream, ENCODING);
                log.info("查到 {} 的进程ID： {}", queryStr, sids);
                return Arrays.stream(sids.split("\\s+"))
                        .filter(pid -> !"ProcessId".equalsIgnoreCase(pid))
                        .map(ConvertUtils::toInteger)
                        .filter(pid -> pid > 0)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("查找进程异常: " + queryStr, e);
        }
        return null;
    }

    @Override
    public List<ProcessInfo> findProcessByName(String name) {
        try {
            log.info("查找 {} 进程信息", name);
            Process process = Runtime.getRuntime().exec("wmic process where Name=\"{name}\" get CreationDate, ProcessId".replace("{name}", name));
            process.waitFor(15, TimeUnit.SECONDS);
            InputStream inputStream = process.getInputStream();
            if(inputStream.available() > 0) {
                List<String> lines = IOUtils.readLines(inputStream, ENCODING);
                if(lines.size() < 2) {
                    return Collections.emptyList();
                }

                String[] captions = lines.get(0).split("\\s+");

                List<ProcessInfo> result = new ArrayList<>(lines.size() - 1);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                for(int i = 1; i < lines.size(); i ++) {
                    String line = lines.get(i);
                    if(StringUtils.isBlank(line)) {
                        continue;
                    }

                    String[] fields = line.split("\\s+");

                    ProcessInfo processInfo = new ProcessInfo();
                    result.add(processInfo);
                    for(int f = 0; f < captions.length; f ++) {
                        if("CreationDate".equalsIgnoreCase(captions[f])) {
                            processInfo.setCreationData(dateFormat.parse(fields[f].split("\\.")[0]));
                        } else if("ProcessId".equalsIgnoreCase(captions[f])) {
                            processInfo.setProcessId(ConvertUtils.toInteger(fields[f]));
                        }
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("查找进程异常: " + name, e);
        }
        return Collections.emptyList();
    }

    @Deprecated
    @Override
    public List<String> findProcess(String queryStr) {
        try {
            log.info("查找 {} 的相关进程", queryStr);
            Process process = Runtime.getRuntime().exec("wmic process where \"commandline like '%" + queryStr + "%'\" get ProcessId,caption,commandline");
            process.waitFor(15, TimeUnit.SECONDS);
            InputStream inputStream = process.getInputStream();
            if(inputStream.available() > 0) {
                List<String> lines = IOUtils.readLines(inputStream, ENCODING);
                return lines.stream().skip(1).filter(StringUtils::isNotBlank).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("查找进程异常: " + queryStr, e);
        }
        return Collections.emptyList();
    }

    @Override
    public void showDir(String dir) throws IOException {
        Runtime.getRuntime().exec("explorer.exe " + dir);
    }

    @Override
    public String proxyAPI(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            String response = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
            log.info("绑定911端口-> {}: {}", cmd, response);
            return response;
        } catch (IOException e) {
            log.error(cmd + " 执行异常", e);
            throw new RuntimeException("911拨号失败: " + e.getMessage());
        }
    }

}
