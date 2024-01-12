package com.tencent.devops.quality.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "质量红线-把关操作记录")
data class QualityRuleBuildHisOpt(
    @Schema(description = "红线hashId", required = true)
    val ruleHashId: String,
    @Schema(description = "红线把关人", required = false)
    val gateKeepers: List<String>? = null,
    @Schema(description = "stageId", required = false)
    val stageId: String? = "",
    @Schema(description = "操作人", required = false)
    val gateOptUser: String? = "",
    @Schema(description = "操作时间", required = false)
    val gateOptTime: String? = ""
)
