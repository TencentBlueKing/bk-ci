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

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Github Review 事件")
data class GithubReviewEvent(
    val action: String,
    @JsonProperty("pull_request")
    @Schema(title = "Issues相关信息")
    val pullRequest: GithubPullRequest,
    @Schema(title = "Github仓库相关信息")
    val repository: GithubRepository,
    @Schema(title = "操作人信息")
    override val sender: GithubUser,
    @Schema(title = "评审信息")
    val review: GithubReview
) : GithubEvent(sender) {
    companion object {
        const val classType = "pull_request_review"
    }

    fun convertState() = when {
        isApproved() -> "approved"
        isChangeRequired() -> "change_required"
        isChangeDenied() -> "change_denied"
        isApproving() -> "approving"
        else -> ""
    }

    // 当存在多个必要评审人时,一个用户评审通过不算通过,需判断合并状态
    private fun isApproved() = (pullRequest.mergeable == true) &&
            ((pullRequest.mergeableState?.toUpperCase() ?: "") == ReviewMergeStateStatus.CLEAN.name)

    private fun isChangeRequired() = review.state == GithubReviewState.CHANGES_REQUESTED.value

    private fun isChangeDenied() = action == GithubReviewState.DISMISSED.value

    // 当存在多个必要评审人时,一个用户评审通过但仍不允许merge，次状态为approving
    private fun isApproving() =
        (review.state == GithubReviewState.APPROVING.value || review.state == GithubReviewState.APPROVED.value) &&
            !isApproved()
}
data class GithubReview(
    override val id: Long,
    @JsonProperty("node_id")
    override val nodeId: String,
    @JsonProperty("html_url")
    @Schema(title = "评审地址[网页地址]")
    override val htmlUrl: String?,
    @JsonProperty("created_at")
    override val createdAt: String?, // 2022-06-21T08:45:41Z
    @JsonProperty("updated_at")
    override val updatedAt: String?, // 2022-06-21T08:45:41Z
    val user: GithubUser,
    @Schema(title = "评审内容")
    val body: String?,
    @JsonProperty("commit_id")
    val commitId: String,
    @Schema(title = "提交时间")
    @JsonProperty("submitted_at")
    val submittedAt: String,
    @Schema(title = "评审状态")
    val state: String,
    @JsonProperty("pull_request_url")
    @Schema(title = "PR地址[Api地址]")
    val pullRequestUrl: String,
    @JsonProperty("author_association")
    val authorAssociation: String
) : GithubBaseInfo(
    id = id,
    htmlUrl = htmlUrl,
    nodeId = nodeId,
    updatedAt = updatedAt,
    createdAt = createdAt
)

@Schema(title = "Github Review 状态")
enum class GithubReviewState(val value: String) {
    @Schema(title = "批准")
    APPROVED("approved"),

    @Schema(title = "要求修改")
    CHANGES_REQUESTED("changes_requested"),

    @Schema(title = "评论")
    COMMENTED("commented"),

    @Schema(title = "驳回")
    DISMISSED("dismissed"),

    @Schema(title = "尚未提交的评审报告")
    PENDING("pending"),

    @Schema(title = "评审中【自定义枚举项，实际不存在】")
    APPROVING("approving")
}

@Schema(title = "Github Review 合并状态")
enum class ReviewMergeStateStatus {
    @Schema(title = "头标已过时")
    BEHIND,
    @Schema(title = "阻塞")
    BLOCKED,
    @Schema(title = "可合并和传递提交状态")
    CLEAN,
    @Schema(title = "无法干净地创建合并提交")
    DIRTY,
    @Schema(title = "由于拉取请求是草稿")
    DRAFT,
    @Schema(title = "可与传递的提交状态和预接收挂钩合并")
    HAS_HOOKS,
    @Schema(title = "目前无法确定状态")
    UNKNOWN,
    @Schema(title = "可与非传递提交状态合并")
    UNSTABLE
}
