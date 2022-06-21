package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class Links(
    val self: Link,
    val html: Link,
    val issue: Link,
    val comments: Link,
    @JsonProperty("review_comments")
    val reviewComments: Link,
    @JsonProperty("review_comment")
    val reviewComment: Link,
    val commits: Link,
    val statuses: Link
)
