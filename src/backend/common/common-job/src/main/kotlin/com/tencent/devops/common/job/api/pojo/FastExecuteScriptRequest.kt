package com.tencent.devops.common.job.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.stereotype.Component

@Component
data class FastExecuteScriptRequest(
    val userId: String,
    val scriptContent: String,
    val scriptTimeout: Long,
    val scriptParam: String? = null,
    @JsonProperty("isParamSensive")
    val paramSensive: Int,
    val scriptType: Int,
    val envSet: EnvSet,
    val account: String
)