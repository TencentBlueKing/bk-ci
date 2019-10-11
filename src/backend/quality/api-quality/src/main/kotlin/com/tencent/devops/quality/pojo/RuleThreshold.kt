package com.tencent.devops.quality.pojo

import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-拦截规则阈值")
data class RuleThreshold(
    @ApiModelProperty("指标ID", required = true)
    val metadataId: String,
    @ApiModelProperty("指标名称", required = true)
    val metadataName: String,
    @ApiModelProperty("关系", required = true)
    val operation: QualityOperation,
    @ApiModelProperty("阈值值大小", required = true)
    val value: String
)