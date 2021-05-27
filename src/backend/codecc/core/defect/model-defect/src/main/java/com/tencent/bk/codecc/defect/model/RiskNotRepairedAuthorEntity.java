package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

/**
 * “风险系列”处理人统计
 */
@Data
public class RiskNotRepairedAuthorEntity {
    /**
     * 处理人
     */
    @Field("name")
    private String name;

    /**
     * 极高风险级别告警数
     */
    @Field("super_high_count")
    private int superHighCount;

    /**
     * 高风险级别告警数
     */
    @Field("high_count")
    private int highCount;

    /**
     * 中风险级别告警数
     */
    @Field("medium_count")
    private int mediumCount;
}
