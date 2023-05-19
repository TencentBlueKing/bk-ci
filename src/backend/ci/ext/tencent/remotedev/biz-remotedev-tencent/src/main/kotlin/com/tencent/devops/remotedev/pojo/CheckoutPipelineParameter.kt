package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class CheckoutPipelineParameter(
    @JsonProperty("node_displayName")
    val nodeDisplayName: String,
    @JsonProperty("git_url")
    val gitUrl: String,
    @JsonProperty("git_branch")
    val gitBranch: String,
    @JsonProperty("git_save_path")
    val gitSavePath: String,
)
