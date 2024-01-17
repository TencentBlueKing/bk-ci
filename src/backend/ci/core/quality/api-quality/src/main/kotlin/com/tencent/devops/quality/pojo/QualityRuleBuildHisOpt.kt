package com.tencent.devops.quality.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "质量红线-把关操作记录")
data class QualityRuleBuildHisOpt(
    @Schema(title = "红线hashId", required = true)
    val ruleHashId: String,
    @Schema(title = "红线把关人", required = false)
    val gateKeepers: List<String>? = null,
    @Schema(title = "stageId", required = false)
    val stageId: String? = "",
    @Schema(title = "操作人", required = false)
    val gateOptUser: String? = "",
    @Schema(title = "操作时间", required = false)
    val gateOptTime: String? = ""
)
