package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-拦截规则拦截记录")
data class QualityRuleInterceptRecord(
    @ApiModelProperty("指标ID", required = true)
    val indicatorId: String,
    @ApiModelProperty("指标名称", required = true)
    val indicatorName: String,
    @ApiModelProperty("指标插件类型", required = false)
    val indicatorType: String?,
    @ApiModelProperty("关系", required = true)
    val operation: QualityOperation,
    @ApiModelProperty("阈值值大小", required = true)
    val value: String?,
    @ApiModelProperty("实际值", required = true)
    val actualValue: String?,
    @ApiModelProperty("控制点", required = true)
    val controlPoint: String,
    @ApiModelProperty("是否通过", required = true)
    val pass: Boolean,
    @ApiModelProperty("指标详情", required = true)
    val detail: String?,
    @ApiModelProperty("指标日志输出详情", required = false)
    val logPrompt: String?
)