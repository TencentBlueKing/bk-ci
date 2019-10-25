package com.tencent.devops.common.cos.response;

import com.tencent.devops.common.cos.model.exception.COSException;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by liangyuzhou on 2017/2/10.
 * Powered By Tencent
 */
public class BaseResponse {
    private boolean success;
    private String errorMessage;
    private final Logger logger = LoggerFactory.getLogger(BaseResponse.class);

    public void parseResponse(final Response response) throws COSException {
        if (response.isSuccessful()) {
            setSuccess(true);
        } else {
            setSuccess(false);
            setCommonErrorMessage(response.code());
            logger.error("COS response code:" + response.code());
            logger.error("COS response header:" + response.headers().toString());
        }
    }

    protected void setCommonErrorMessage(int code) {
        switch (code) {
            case 401:
                setErrorMessage("Invalid signature");
                break;
            case 404:
                setErrorMessage("Invalid resource");
                break;
            case 403:
                setErrorMessage("Not permitted");
                break;
            case 500:
                setErrorMessage("COS system error");
                break;
            default:
                setErrorMessage("Unknown COS error");
                break;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


}
