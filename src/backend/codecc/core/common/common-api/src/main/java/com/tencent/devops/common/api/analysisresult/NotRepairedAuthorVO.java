package com.tencent.devops.common.api.analysisresult;

import lombok.Data;

@Data
public class NotRepairedAuthorVO {
    /**
     * 处理人名字
     */
    private String name;

    /**
     * 严重告警数
     */
    private int seriousCount;

    /**
     * 一般告警数
     */
    private int normalCount;

    /**
     * 提示告警数
     */
    private int promptCount;

    /**
     * 告警总数
     */
    private int totalCount;
}
