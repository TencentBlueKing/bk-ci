package com.tencent.devops.repository.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class RequiredStatusChecks(
    @JsonProperty("enforcement_level")
    val enforcementLevel: String,
    val contexts: List<String>
)
