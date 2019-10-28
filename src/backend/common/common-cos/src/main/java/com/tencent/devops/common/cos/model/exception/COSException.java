package com.tencent.devops.common.cos.model.exception;

/**
 * Created by liangyuzhou on 2017/2/9.
 * Powered By Tencent
 */
public class COSException extends Exception {
    private static final long serialVersionUID = 6211285416290345898L;

    public COSException(final String message) {
        super(message);
    }

    public COSException(final String message, final Throwable t) {
        super(message, t);
    }
}
