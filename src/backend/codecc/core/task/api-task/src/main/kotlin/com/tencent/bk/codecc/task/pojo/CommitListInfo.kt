package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class CommitListInfo(
    @JsonProperty("id")
    val id: String?,
    @JsonProperty("short_id")
    val shortId: String?,
    @JsonProperty("title")
    val title: String?,
    @JsonProperty("author_name")
    val authorName: String?,
    @JsonProperty("author_email")
    val authorEmail: String?,
    @JsonProperty("created_at")
    val createdAt: String?,
    @JsonProperty("message")
    val message: String?
)