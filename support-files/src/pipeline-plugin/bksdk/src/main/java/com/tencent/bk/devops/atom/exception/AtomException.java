package com.tencent.bk.devops.atom.exception;

/**
 * @version 1.0
 */
@SuppressWarnings("unused")
public class AtomException extends RuntimeException {

    public AtomException() {
    }

    public AtomException(String message) {
        super(message);
    }

    public AtomException(String message, Throwable cause) {
        super(message, cause);
    }

    public AtomException(Throwable cause) {
        super(cause);
    }

    public AtomException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
