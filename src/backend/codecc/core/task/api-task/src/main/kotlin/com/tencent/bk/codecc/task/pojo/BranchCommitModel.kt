package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BranchCommitModel(
    val id: String,
    val message: String,
    @JsonProperty("parent_ids")
    val parentIds: List<String>,
    @JsonProperty("authored_date")
    val authoredDate: String,
    @JsonProperty("author_name")
    val authorName: String,
    @JsonProperty("author_email")
    val authorEmail: String,
    @JsonProperty("committed_date")
    val committedDate: String,
    @JsonProperty("committer_name")
    val committerName: String,
    @JsonProperty("committer_email")
    val committerEmail: String,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("short_id")
    val shortId: String
)