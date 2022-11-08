package com.ruiyun.jvppeteer.util.os;

import com.ruiyun.jvppeteer.util.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LinuxCmd implements OSCommand {

    private static final String ENCODING = "UTF-8";

    @Override
    public String chromeAppName() {
        return "chrome";
    }

    @Override
    public void killPID(Serializable pid) {
        if(pid == null) {
            return;
        }
        try {
            log.info("Kill 进程PID: " + pid);
            Process process = Runtime.getRuntime().exec("kill -9 " + pid);
            process.waitFor(15, TimeUnit.SECONDS);
            echo(process);
        } catch (Exception e) {
            log.error("杀掉进程PID" + pid + "异常", e);
        }
    }

    @Override
    public void killPIDs(Serializable pid) {
        killPID(pid);
    }

    @Override
    public boolean isStarted() throws IOException {
        return false;
    }

    @Override
    public boolean releasePort(int port, String... ignorePIDs) throws IOException {
        return false;
    }

    @Override
    public void killChromeDriver() {
    }

    @Override
    public void killChrome() {

    }

    @Override
    public void killMultilogin() {

    }

    @Override
    public void killByQueryStr(String queryStr) {

    }

    @Override
    public List<Integer> findProcessIds(String queryStr) {
        return null;
    }

    @Override
    public List<ProcessInfo> findProcessByName(String name) {
        return null;
    }

    @Override
    public List<String> findProcess(String queryStr) {
        return null;
    }

    @Override
    public void showDir(String dir) throws IOException {
        Runtime.getRuntime().exec("nautilus " + dir);
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
}
