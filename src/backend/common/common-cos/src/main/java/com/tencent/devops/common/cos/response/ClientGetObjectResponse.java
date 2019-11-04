package com.tencent.devops.common.cos.response;

import okhttp3.Response;

public class ClientGetObjectResponse extends BaseResponse {
    private String url;

    @Override
    public void parseResponse(Response response) {
        setSuccess(false);
        setErrorMessage("Invalid method call");
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
