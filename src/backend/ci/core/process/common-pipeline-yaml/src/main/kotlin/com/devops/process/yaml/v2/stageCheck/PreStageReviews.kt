package com.devops.process.yaml.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PreStageReviews(
    val flows: List<PreFlow>?,
    val variables: Map<String, ReviewVariable>?,
    val description: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PreFlow(
    val name: String,
    val reviewers: Any
)
