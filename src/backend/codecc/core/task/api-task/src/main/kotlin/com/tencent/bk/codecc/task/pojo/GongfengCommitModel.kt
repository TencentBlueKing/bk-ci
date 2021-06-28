package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GongfengCommitModel(
    val id: String,
    @JsonProperty("short_id")
    val shortId: String,
    val title: String,
    @JsonProperty("author_name")
    val authorName: String,
    @JsonProperty("author_email")
    val authorEmail: String,
    @JsonProperty("created_at")
    val createdAt: String,
    val message: String
)