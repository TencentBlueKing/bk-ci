package com.tencent.devops.common.itest.api.response;

import com.tencent.devops.common.itest.api.pojo.VersionOnly;

import java.util.List;

public class VersionGetVersionOnlyResponse extends BaseResponse {
    private String code;
    private String message;
    private List<VersionOnly> data;

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

    public List<VersionOnly> getData() {
        return data;
    }

    public void setData(List<VersionOnly> data) {
        this.data = data;
    }
}
