package com.tencent.devops.common.itest.api.response;

import java.util.Map;

public class ProcessTestMasterResponse extends BaseResponse {
    private String code;
    private String message;
    private Map<String, String> data;

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

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
