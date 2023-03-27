package com.tencent.devops.dispatch.windows.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

@ApiModel("DevCloud获取windows所需的env信息")
data class DevCloudWindowsCreateEnv(
    var project: String,
    var pipelineId: String,
    var buildId: String,
    var vmSeqId: String,
    var devops_project_id: String,
    var devops_agent_id: String,
    var devops_agent_secret_key: String,
    var devops_gateway: String,
    @JsonProperty("landun.env")
    var landun_env: String
)
