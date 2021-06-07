package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class ActiveProjParseModel(
    val id: Int,
    val type: String,
    val owners: String,
    val creator: String,
    @JsonProperty("git_path")
    val gitPath: String,
    @JsonProperty("hook_url")
    val hookUrl: String?,
    @JsonProperty("created_at")
    val createdAt: String?,
    @JsonProperty("owners_org")
    val ownersOrg: String,
    @JsonProperty("push_count")
    val pushCount: Int?

)
