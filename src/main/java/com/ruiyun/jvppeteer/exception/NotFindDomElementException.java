package com.ruiyun.jvppeteer.exception;

public class NotFindDomElementException extends Exception {

    private String selector;

    public NotFindDomElementException() {
    }

    public NotFindDomElementException(String message) {
        super(message);
    }
    public NotFindDomElementException(String selector, String message) {
        this(message + "selector【" + selector + "】");
        this.selector = selector;
    }

}
