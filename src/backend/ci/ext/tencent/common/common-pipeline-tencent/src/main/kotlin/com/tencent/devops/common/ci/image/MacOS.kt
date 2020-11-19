package com.tencent.devops.common.ci.image

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MacOS(
    val systemVersion: String?,
    val xcodeVersion: String?
)