package com.tencent.devops.remotedev.pojo.startcloud

import com.fasterxml.jackson.annotation.JsonProperty

data class StartCloudKickInstanceReq(
    @JsonProperty("userId")
    val userId: String,

    @JsonProperty("envId")
    val envId: String
)
