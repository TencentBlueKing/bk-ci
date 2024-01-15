package com.tencent.devops.quality.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "质量红线-把关操作记录")
data class QualityRuleBuildHisOpt(
    @Schema(name = "红线hashId", required = true)
    val ruleHashId: String,
    @Schema(name = "红线把关人", required = false)
    val gateKeepers: List<String>? = null,
    @Schema(name = "stageId", required = false)
    val stageId: String? = "",
    @Schema(name = "操作人", required = false)
    val gateOptUser: String? = "",
    @Schema(name = "操作时间", required = false)
    val gateOptTime: String? = ""
)
