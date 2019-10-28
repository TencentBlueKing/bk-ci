package com.tencent.devops.common.cos.response;

/**
 * Created by schellingma on 2017/04/21.
 * Powered By Tencent
 */
public class DeleteObjectResponse extends BaseResponse {

    private boolean isNotFound = false;

    @Override
    protected void setCommonErrorMessage(int code) {
        isNotFound = false;

        switch (code) {
            case 401:
                setErrorMessage("Invalid signature");
                break;
            case 404:
                isNotFound = true;
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

    public boolean getIsNotFound() {
        return isNotFound;
    }

}
