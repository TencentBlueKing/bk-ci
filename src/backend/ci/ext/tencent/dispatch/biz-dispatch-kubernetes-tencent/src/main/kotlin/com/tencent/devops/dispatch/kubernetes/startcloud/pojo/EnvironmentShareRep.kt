package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentShareRep(
    @JsonProperty("code")
    val code: Int,
    @JsonProperty("data")
    val data: Data,
    @JsonProperty("message")
    val message: String
) {
    data class Data(
        @JsonProperty("resourceId")
        val resourceId: String
    )
}
