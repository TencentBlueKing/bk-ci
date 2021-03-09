package com.tencent.devops.prebuild.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtraParam(
    val codeccScanPath: String? = null,
    val incrementFileList: List<String>? = null,
    val ideVersion: String? = null,
    val pluginVersion: String? = null
)
