package com.tencent.devops.common.cos.response;


import okhttp3.Response;

/**
 * Created by schellingma on 2017/05/08.
 * Powered By Tencent
 */
public class DeleteBucketResponse extends BaseResponse {

    private Boolean existBucket;
    private Boolean hasPermission;

    @Override
    public void parseResponse(Response response) {
        if (response.isSuccessful()) {
            setSuccess(true);
            setExistBucket(true);
            setHasPermission(true);
        } else {
            setSuccess(false);
            int code = response.code();
            switch (code) {
                case 401:
                    setErrorMessage("Invalid signature");
                    break;
                case 404:
                    setSuccess(true);
                    setExistBucket(false);
                    setHasPermission(false);
                    break;
                case 403:
                    setSuccess(true);
                    setExistBucket(true);
                    setHasPermission(false);
                    break;
                case 500:
                    setErrorMessage("COS system error");
                    break;
                default:
                    setErrorMessage("Unknown COS error");
                    break;
            }
        }
    }

    public Boolean isExistBucket() {
        return existBucket;
    }

    private void setExistBucket(Boolean existBucket) {
        this.existBucket = existBucket;
    }

    public Boolean getHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(Boolean hasPermission) {
        this.hasPermission = hasPermission;
    }
}
