package com.tencent.devops.common.itest.api.response;

import com.tencent.devops.common.itest.api.pojo.Task;

public class TaskCreateResponse extends BaseResponse {
    private String code;
    private String message;
    private Task data;

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

    public Task getData() {
        return data;
    }

    public void setData(Task data) {
        this.data = data;
    }
}
