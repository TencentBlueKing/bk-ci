package com.tencent.devops.common.api.analysisresult;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 风险等级类分析基类
 */
@Data
public class BaseRiskAnalysisResultVO extends BaseLastAnalysisResultVO {
    /**
     * 新增极高风险告警数
     */
    @ApiModelProperty("新增极高风险告警数")
    private int newSuperHighCount;

    /**
     * 新增高风险告警数
     */
    @ApiModelProperty("新增高风险告警数")
    private int newHighCount;

    /**
     * 新增中风险告警数
     */
    @ApiModelProperty("新增中风险告警数")
    private int newMediumCount;


    /**
     * 所有极高风险告警数
     */
    @ApiModelProperty("所有极高风险告警数")
    private Integer superHighCount;

    /**
     * 所有高风险告警数
     */
    @ApiModelProperty("所有高风险告警数")
    private Integer highCount;

    /**
     * 所有中高风险告警数
     */
    @ApiModelProperty("所有中高风险告警数")
    private Integer mediumCount;
}
