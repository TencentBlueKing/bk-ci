package com.tencent.devops.common.ci.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.ci.v2.Template

@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTemplateStageCheck(
    val reviews: PreStageReviews?,
    val gates: List<Template>?,
    @JsonProperty("timeout-hours")
    val timeoutHours: Int?
)
