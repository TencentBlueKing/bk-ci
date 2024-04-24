package com.tencent.devops.repository.sdk.github.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GetUserEmailResponse(
    @JsonProperty("email")
    val email: String,
    @JsonProperty("primary")
    val primary: Boolean,
    @JsonProperty("verified")
    val verified: Boolean,
    @JsonProperty("visibility")
    val visibility: String?
)
