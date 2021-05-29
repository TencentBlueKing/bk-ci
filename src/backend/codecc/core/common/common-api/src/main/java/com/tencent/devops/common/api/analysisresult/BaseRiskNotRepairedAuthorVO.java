package com.tencent.devops.common.api.analysisresult;

import lombok.Data;

@Data
public class BaseRiskNotRepairedAuthorVO {
    /**
     * 处理人
     */
    private String name;

    /**
     * 极高风险级别告警数
     */
    private int superHighCount;

    /**
     * 高风险级别告警数
     */
    private int highCount;

    /**
     * 中风险级别告警数
     */
    private int mediumCount;
}
