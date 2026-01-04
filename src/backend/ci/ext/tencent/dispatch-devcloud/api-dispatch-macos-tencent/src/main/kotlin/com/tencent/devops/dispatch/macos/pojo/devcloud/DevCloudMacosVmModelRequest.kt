package com.tencent.devops.dispatch.macos.pojo.devcloud

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * DevCloud macOS VM Model 请求
 */
@Schema(title = "DevCloud macOS VM Model请求")
data class DevCloudMacosVmModelRequest(
    @get:Schema(title = "平台")
    val platform: String,
    @get:Schema(title = "t1参数")
    @JsonProperty("projectId")
    val t1: String,
    @get:Schema(title = "t2参数")
    @JsonProperty("pipelineId")
    val t2: String
)
