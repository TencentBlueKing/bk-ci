package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class Milestone(
    @JsonProperty("closed_at")
    val closedAt: LocalDateTime,
    @JsonProperty("closed_issues")
    val closedIssues: Int,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
    val creator: GithubAuthor,
    val description: String,
    @JsonProperty("due_on")
    val dueOn: LocalDateTime,
    @JsonProperty("html_url")
    val htmlUrl: String,
    val id: Int,
    @JsonProperty("labels_url")
    val labelsUrl: String,
    @JsonProperty("node_id")
    val nodeId: String,
    val number: Int,
    @JsonProperty("open_issues")
    val openIssues: Int,
    val state: String,
    val title: String,
    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,
    val url: String
)