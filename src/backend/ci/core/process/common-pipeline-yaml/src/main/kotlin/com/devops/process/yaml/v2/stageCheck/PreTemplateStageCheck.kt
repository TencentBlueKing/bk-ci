package com.devops.process.yaml.v2.stageCheck

import com.devops.process.yaml.v2.models.Template
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTemplateStageCheck(
    val reviews: PreStageReviews?,
    val gates: List<Template>?,
    @JsonProperty("timeout-hours")
    val timeoutHours: Int?
)
