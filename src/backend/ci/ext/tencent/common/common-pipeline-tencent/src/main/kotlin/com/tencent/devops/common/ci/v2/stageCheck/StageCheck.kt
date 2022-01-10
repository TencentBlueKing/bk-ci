package com.tencent.devops.common.ci.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonProperty

data class StageCheck(
    val reviews: StageReviews?,
    val gates: List<Gate>?,
    @JsonProperty("timeout-hours")
    val timeoutHours: Int?
)
