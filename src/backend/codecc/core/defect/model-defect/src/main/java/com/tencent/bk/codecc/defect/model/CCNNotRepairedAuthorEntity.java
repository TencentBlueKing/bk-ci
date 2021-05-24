package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 圈复杂度处理人信息统计
 */
@Data
public class CCNNotRepairedAuthorEntity extends RiskNotRepairedAuthorEntity {

    /**
     * 低风险级别告警数
     */
    @Field("low_count")
    private int lowCount;
}
