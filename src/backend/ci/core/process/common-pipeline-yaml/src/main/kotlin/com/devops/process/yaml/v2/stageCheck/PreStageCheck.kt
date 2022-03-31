package com.devops.process.yaml.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PreStageCheck(
    val reviews: PreStageReviews?,
    val gates: List<Gate>?,
    @JsonProperty("timeout-hours")
    val timeoutHours: Int?
)
