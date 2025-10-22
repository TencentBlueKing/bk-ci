/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.webhook.pojo.code.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Github 评论分四种：
 * 1. PR Review 评论
 *  1.1 基于PR直接评论
 *  1.2 基于PR Review的会话进行评论
 * 2. Issues 评论
 * 3. Commit 评论
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class GithubCommentEvent(
    open val action: String,
    open val repository: GithubRepository,
    open val comment: GithubComment,
    override val sender: GithubUser
) : GithubEvent(sender) {
    open fun getCommentType(): String = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommitCommentEvent(
    override val action: String,
    override val repository: GithubRepository,
    override val comment: GithubCommitComment,
    override val sender: GithubUser
) : GithubCommentEvent(
    action = action,
    repository = repository,
    comment = comment,
    sender = sender
) {
    companion object {
        const val classType = "commit_comment"

        // 评论类型：基于Commit进行评论
        const val commentType = "Commit"
    }

    override fun getCommentType() = commentType
}

@Schema(title = "Github Review 评论事件")
data class GithubReviewCommentEvent(
    override val action: String,
    @JsonProperty("pull_request")
    @get:Schema(title = "Issues相关信息")
    val pullRequest: GithubPullRequest,
    @get:Schema(title = "Github仓库相关信息")
    override val repository: GithubRepository,
    @get:Schema(title = "操作人信息")
    override val sender: GithubUser,
    @get:Schema(title = "Review会话信息")
    override val comment: GithubReviewComment
) : GithubCommentEvent(
    action = action,
    repository = repository,
    comment = comment,
    sender = sender
) {
    companion object {
        const val classType = "pull_request_review_comment"

        // 评论类型：基于Pull Request Review进行评论
        const val commentType = "Review"
    }

    override fun getCommentType() = commentType
}

data class GithubReviewComment(
    override val id: Long,
    override val url: String?,
    @JsonProperty("html_url")
    override val htmlUrl: String?,
    @JsonProperty("node_id")
    override val nodeId: String,
    override val body: String,
    override val user: GithubUser,
    @JsonProperty("created_at")
    override val createdAt: String?,
    @JsonProperty("updated_at")
    override val updatedAt: String?,
    @get:Schema(title = "Github PR Review Id")
    @JsonProperty("pull_request_review_id")
    val pullRequestReviewId: Long,
    @get:Schema(title = "Github PR Review会话对应的文件")
    val path: String
) : GithubComment(
    id = id,
    url = url,
    htmlUrl = htmlUrl,
    nodeId = nodeId,
    body = body,
    user = user,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubIssueCommentEvent(
    override val action: String,
    override val repository: GithubRepository,
    override val comment: GithubIssueComment,
    override val sender: GithubUser,
    val issue: GithubIssue
) : GithubCommentEvent(
    action = action,
    repository = repository,
    comment = comment,
    sender = sender
) {
    companion object {
        const val classType = "issue_comment"
    }

    override fun getCommentType() = if (issue.pullRequest != null) {
        // 对齐Code_Git直接在PR上评论则[noteableType=Review]
        "Review"
    } else {
        "Issue"
    }
}

@Suppress("LongParameterList")
@Schema(title = "Github 评论信息父类")
abstract class GithubComment(
    @get:Schema(title = "评论ID")
    override val id: Long,
    @get:Schema(title = "评论链接[API链接]")
    override val url: String?,
    @JsonProperty("html_url")
    @get:Schema(title = "评论链接[网页链接]")
    override val htmlUrl: String?,
    @JsonProperty("node_id")
    override val nodeId: String,
    @get:Schema(title = "评论内容")
    open val body: String,
    @get:Schema(title = "评论的用户")
    open val user: GithubUser,
    @JsonProperty("created_at")
    @get:Schema(title = "创建时间")
    override val createdAt: String?, // 2022-06-21T08:45:41Z
    @JsonProperty("updated_at")
    @get:Schema(title = "修改时间")
    override val updatedAt: String? // 2022-06-21T08:45:41Z
) : GithubBaseInfo(
    id = id,
    url = url,
    htmlUrl = htmlUrl,
    nodeId = nodeId,
    updatedAt = updatedAt,
    createdAt = createdAt
)

@SuppressWarnings("LongParameterList")
open class GithubCommitComment(
    override val id: Long,
    override val url: String?,
    @JsonProperty("html_url")
    override val htmlUrl: String?,
    @JsonProperty("node_id")
    override val nodeId: String,
    override val body: String,
    override val user: GithubUser,
    @JsonProperty("created_at")
    override val createdAt: String?,
    @JsonProperty("updated_at")
    override val updatedAt: String?,
    @JsonProperty("commit_id")
    @get:Schema(title = "commit sha")
    val commitId: String
) : GithubComment(
    id = id,
    url = url,
    htmlUrl = htmlUrl,
    nodeId = nodeId,
    body = body,
    user = user,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@Schema(title = "Github Issue 评论")
data class GithubIssueComment(
    override val id: Long,
    override val url: String?,
    @JsonProperty("html_url")
    override val htmlUrl: String?,
    @JsonProperty("node_id")
    override val nodeId: String,
    override val body: String,
    override val user: GithubUser,
    @JsonProperty("created_at")
    override val createdAt: String?,
    @JsonProperty("updated_at")
    override val updatedAt: String?,
    @JsonProperty("issue_url")
    @get:Schema(title = "评论链接[API链接]")
    val issueUrl: String
) : GithubComment(
    id = id,
    url = url,
    htmlUrl = htmlUrl,
    nodeId = nodeId,
    body = body,
    user = user,
    createdAt = createdAt,
    updatedAt = updatedAt
)
