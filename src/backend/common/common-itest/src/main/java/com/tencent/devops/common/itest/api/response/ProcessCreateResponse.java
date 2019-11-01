package com.tencent.devops.common.itest.api.response;

import com.tencent.devops.common.itest.api.pojo.Process;
import com.tencent.devops.common.itest.api.pojo.Task;

public class ProcessCreateResponse extends BaseResponse {
    private String code;
    private String message;
    private Process data;

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

    public Process getData() {
        return data;
    }

    public void setData(Process data) {
        this.data = data;
    }
}
