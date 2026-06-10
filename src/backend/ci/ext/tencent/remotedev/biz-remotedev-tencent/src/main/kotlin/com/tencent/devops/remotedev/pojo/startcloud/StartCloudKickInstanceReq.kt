package com.tencent.devops.remotedev.pojo.startcloud

import com.fasterxml.jackson.annotation.JsonProperty

data class StartCloudKickInstanceReq(
    @JsonProperty("user_id")
    val userId: String,

    @JsonProperty("env_id")
    val envId: String
)
