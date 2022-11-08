package com.ruiyun.jvppeteer.util.os;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public interface OSCommand {
    Random random = new Random();
    int FIRST_PORT = 9500;
    int LAST_PORT = 65535;

    String chromeAppName();

    /**
     * 获取CPU核心数
     */
    default int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }


    default int findFreePort() {
        return findFreePort(FIRST_PORT, LAST_PORT);
    }
    default int findFreePort(int portMin,int portMax) {
        return findFreePort(portMin, portMax, null);
    }
    default int findFreePort(int portMin,int portMax, Collection<Integer> excludePorts) {
        o: for (int i = 0; i < 5; i++) {
            int seedPort;

            int j = 0;
            do {
                j ++;
                seedPort = portMin + random.nextInt(portMax - portMin);
                if(excludePorts == null || !excludePorts.contains(seedPort)) {
                    break;
                }

                if(j > 5) {
                    continue o;
                }
            } while (true);

            int suggestedPort = checkPortIsFree(seedPort);
            if (suggestedPort != -1) {
                return suggestedPort;
            }
        }
        throw new RuntimeException("Unable to find a free port");
    }

    /**
     * 检测端口是否自由
     * @param port
     * @return
     */
    default int checkPortIsFree(int port) {
        /*try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("localhost", port));
            return socket.getLocalPort();
        } catch (IOException e) {
            return -1;
        }*/
        return checkIpPort("localhost", port) ? -1 : port;
    }

    /**
     * 检测Ip和端口是否可用
     *
     * @param ip
     * @param port
     * @return
     */
    default boolean checkIpPort(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip,port),3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    void killPID(Serializable pid);

    void killPIDs(Serializable pid);

    boolean isStarted() throws IOException;

    boolean releasePort(int port, String... ignorePIDs) throws IOException;

    void killChromeDriver();

    void killChrome();

    void killMultilogin();

    void killByQueryStr(String queryStr);

    List<Integer> findProcessIds(String queryStr);

    List<ProcessInfo> findProcessByName(String name);

    @Deprecated
    List<String> findProcess(String queryStr);

    void showDir(String dir) throws IOException;

    default String proxyAPI(String cmd) {
        throw new UnsupportedOperationException("当前系统尚未集成911");
    }
}
