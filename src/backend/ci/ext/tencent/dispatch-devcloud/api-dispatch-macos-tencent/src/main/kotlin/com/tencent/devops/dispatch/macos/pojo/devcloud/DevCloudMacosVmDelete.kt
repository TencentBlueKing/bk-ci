package com.tencent.devops.dispatch.macos.pojo.devcloud

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DevCloud删除VM")
data class DevCloudMacosVmDelete(
    var project: String,
    var pipelineId: String,
    var buildId: String,
    var vmSeqId: String,
    @JsonProperty("id")
    var id: String
)
