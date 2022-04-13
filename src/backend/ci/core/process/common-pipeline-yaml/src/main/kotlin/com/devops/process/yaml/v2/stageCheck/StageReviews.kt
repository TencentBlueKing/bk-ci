package com.devops.process.yaml.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StageReviews(
    val flows: List<Flow>?,
    val variables: Map<String, ReviewVariable>?,
    val description: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Flow(
    val name: String,
    val reviewers: List<String>
)
