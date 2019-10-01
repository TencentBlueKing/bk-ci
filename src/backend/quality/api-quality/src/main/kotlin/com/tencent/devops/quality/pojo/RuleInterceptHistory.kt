package com.tencent.devops.quality.pojo

import com.tencent.devops.quality.pojo.enum.RuleInterceptResult
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-拦截记录")
data class RuleInterceptHistory(
    @ApiModelProperty("hashId", required = true)
    val hashId: String,
    @ApiModelProperty("项目里的序号", required = true)
    val num: Long,
    @ApiModelProperty("时间戳(秒)", required = true)
    val timestamp: Long,
    @ApiModelProperty("拦截结果", required = true)
    val interceptResult: RuleInterceptResult,
    @ApiModelProperty("规则HashId", required = true)
    val ruleHashId: String,
    @ApiModelProperty("规则名称", required = true)
    val ruleName: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    val pipelineName: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建号", required = true)
    val buildNo: String,
    @ApiModelProperty("描述", required = true)
    val remark: String,
    @ApiModelProperty("流水线是否已删除", required = true)
    val pipelineIsDelete: Boolean = false
)