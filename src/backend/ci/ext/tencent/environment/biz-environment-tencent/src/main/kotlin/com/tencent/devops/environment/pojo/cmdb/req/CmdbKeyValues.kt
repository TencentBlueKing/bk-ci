package com.tencent.devops.environment.pojo.cmdb.req

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CmdbKeyValues(
    @get:Schema(title = "主机ip列表")
    @JsonProperty("SvrIp")
    val svrIpStrList: String? = null,
    @get:Schema(title = "主机serverId列表")
    @JsonProperty("serverId")
    val serverIdStrList: String? = null
)
