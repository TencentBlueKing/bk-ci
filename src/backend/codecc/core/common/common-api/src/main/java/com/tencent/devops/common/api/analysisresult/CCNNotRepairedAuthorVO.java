package com.tencent.devops.common.api.analysisresult;

import lombok.Data;

@Data
public class CCNNotRepairedAuthorVO extends BaseRiskNotRepairedAuthorVO {
    /**
     * 低风险级别告警数
     */
    private int lowCount;
}
