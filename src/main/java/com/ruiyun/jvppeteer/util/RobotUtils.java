package com.ruiyun.jvppeteer.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.Random;

@Slf4j
public class RobotUtils {
    private static Robot robot;
    private static final Random random = new Random();

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void randomMouseMove(int count) {
        if(count == 0) {
            return;
        }
        log.info("随机滑动鼠标{}次", count);
        for(int i = 0; i < count; i ++) {
            ThreadUtils.sleep(100 + random.nextInt(200));
            robot.mouseMove(random.nextInt(1000), random.nextInt(900));
        }
    }
}
