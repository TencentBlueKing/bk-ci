package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-拦截检测结果")
data class RuleCheckResult(
    @ApiModelProperty("是否通过", required = true)
    val success: Boolean,
    @ApiModelProperty("失败后是否结束", required = true)
    val failEnd: Boolean,
    @ApiModelProperty("审核超时时间", required = true)
    val auditTimeoutSeconds: Int,
    @ApiModelProperty("失败信息", required = true)
    val resultList: List<RuleCheckSingleResult>
)