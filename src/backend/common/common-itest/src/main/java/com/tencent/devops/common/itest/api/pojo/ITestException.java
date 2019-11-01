package com.tencent.devops.common.itest.api.pojo;

/**
 * Created by liangyuzhou on 2017/2/9.
 * Powered By Tencent
 */
public class ITestException extends Exception {
    private static final long serialVersionUID = 1L;

    public ITestException(final String message) {
        super(message);
    }

    public ITestException(final String message, final Throwable t) {
        super(message, t);
    }
}
