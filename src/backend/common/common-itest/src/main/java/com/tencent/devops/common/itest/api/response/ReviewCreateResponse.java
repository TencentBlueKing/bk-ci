package com.tencent.devops.common.itest.api.response;

import com.tencent.devops.common.itest.api.pojo.Review;

public class ReviewCreateResponse extends BaseResponse {
    private String code;
    private String message;
    private Review data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Review getData() {
        return data;
    }

    public void setData(Review data) {
        this.data = data;
    }
}
