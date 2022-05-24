package com.tencent.devops.common.storage.vo;

import lombok.Data;

@Data
public class BkRepoStartChunkVo {
    /**
     * 	分块上传id
     */
    private String uploadId;
    /**
     * 上传有效期(秒)
     */
    private Long expireSeconds;
}
