package com.tencent.devops.common.cos.response;

import okhttp3.Response;

/**
 * Created by liangyuzhou on 2017/2/13.
 * Powered By Tencent
 */
public class PutObjectResponse extends BaseResponse {
    private String sha1;

    @Override
    public void parseResponse(Response response) {
        if (response.isSuccessful()) {
            setSuccess(true);
            setSha1(response.header("ETag", ""));
        } else {
            setSuccess(false);
            setCommonErrorMessage(response.code());
        }
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
}
