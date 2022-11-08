package com.ruiyun.jvppeteer.exception;

public class BrowserAlreadyClosedException extends RuntimeException {
    public BrowserAlreadyClosedException() {
        super();
    }

    public BrowserAlreadyClosedException(String message) {
        super(message);
    }

    public BrowserAlreadyClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BrowserAlreadyClosedException(Throwable cause) {
        super(cause);
    }

    protected BrowserAlreadyClosedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
