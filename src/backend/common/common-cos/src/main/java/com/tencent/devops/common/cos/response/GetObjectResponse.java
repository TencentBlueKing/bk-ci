package com.tencent.devops.common.cos.response;

import okhttp3.Response;

import java.io.IOException;

/**
 * Created by liangyuzhou on 2017/2/13.
 * Powered By Tencent
 */
public class GetObjectResponse extends BaseResponse {
    private byte[] content;

    @Override
    public void parseResponse(Response response) {
        if (response.isSuccessful()) {
            try {
                setSuccess(true);
                setContent(response.body().bytes());
            } catch (IOException e) {
                setSuccess(false);
                setErrorMessage(e.getMessage());
            }
        } else {
            setSuccess(false);
            setCommonErrorMessage(response.code());
        }
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
