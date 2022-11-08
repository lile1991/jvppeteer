package com.ruiyun.jvppeteer.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class ThreadUtils {

    private static final Random random = new Random();

    public static void sleep(long millis) {
        if(millis >= 1000 * 1000) {
            log.warn("线程sleep时间" + millis + "ms较长, 请注意时间单位!");
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }
    /**
     * 	随机休眠
     * @param millis
     * @param millisMax
     */
    public static void sleep(long millis,long millisMax) {
    	if(millis >= 1000 * 1000) {
    		log.warn("线程sleep时间" + millis + "ms较长, 请注意时间单位!");
    	}

    	long mil = millis + random.nextInt((int) (millisMax - millis));
    	log.debug("随机休眠时长:" + mil);
    	try {
    		Thread.sleep(mil);
    	} catch (InterruptedException ignore) {
    	}
    }
}
