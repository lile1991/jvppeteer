package com.ruiyun.jvppeteer.util.os;

public class OS {
    public enum PlatformEnum {
        MAC, LINUX, WINDOWS;

    }

    private static OSCommand osCommand;
    private static PlatformEnum platform;

    public static PlatformEnum platform() {
        if(platform == null) {
            String os = System.getProperty("os.name");
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Mac OS")) {
                platform = PlatformEnum.MAC;
            } else if (osName.startsWith("Windows")) {
                platform = PlatformEnum.WINDOWS;
            } else {
                platform = PlatformEnum.LINUX;
            }
        }
        return platform;
    }

    public static OSCommand command() {
        if(osCommand != null) {
            return osCommand;
        }

        synchronized (OS.class) {
            switch (platform()) {
                case MAC: osCommand = new MacCmd(); break;
                case WINDOWS: osCommand = new Win32Cmd(); break;
                case LINUX: osCommand = new LinuxCmd(); break;
            }
        }
        return osCommand;
    }
}
