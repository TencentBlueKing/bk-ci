package com.tencent.devops.common.storage.vo;

import lombok.Data;

@Data
public class BkRepoResult<T> {

    private Integer code;

    private String message;

    private T data;

    public Boolean isOk(){
        return code != null && code == 0;
    }
}
