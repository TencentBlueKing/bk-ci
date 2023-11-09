package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentUserCreate(
    @JsonProperty("UserID")
    val userId: String,
    @JsonProperty("ContentProviderName")
    val appName: String
)
