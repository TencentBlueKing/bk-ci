package com.tencent.devops.common.pipeline.pojo.transfer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTemplateVariable(
    val value: Any,
    @JsonProperty("allow-modify-at-startup")
    val allowModifyAtStartup: Boolean? = true
)
