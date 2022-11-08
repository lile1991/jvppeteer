package com.ruiyun.jvppeteer.util.os;

import lombok.Data;

import java.util.Date;

/**
 * 进程信息
 */
@Data
public class ProcessInfo {
    /** 进程ID*/
    private Integer processId;
    /** 创建时间*/
    private Date creationData;
    /** 进程名 */
    private String name;
}
