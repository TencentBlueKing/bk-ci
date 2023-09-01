/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Github Issues 事件")
data class GithubIssuesEvent(
    val action: String,
    @ApiModelProperty("Issues相关信息")
    val issue: GithubIssue,
    @ApiModelProperty("Github仓库相关信息")
    val repository: GithubRepository,
    @ApiModelProperty("操作人信息")
    override val sender: GithubUser,
    @ApiModelProperty("受理人")
    val assignees: List<GithubUser>?
) : GithubEvent(sender) {
    companion object {
        const val classType = "issues"
    }

    fun convertAction() = when (GithubIssuesAction.valueOf(action)) {
        GithubIssuesAction.CREATED -> "open"
        GithubIssuesAction.CLOSED -> "close"
        GithubIssuesAction.REOPENED -> "reopen"
        GithubIssuesAction.EDITED -> "update"
        else -> ""
    }
}

data class GithubIssue(
    override val url: String,
    @JsonProperty("html_url")
    @ApiModelProperty("Issue/Pull Request链接[网页链接]")
    override val htmlUrl: String,
    @ApiModelProperty("Issue ID")
    override val id: Long,
    @JsonProperty("node_id")
    override val nodeId: String,
    @JsonProperty("created_at")
    override val createdAt: String?, // 2022-06-21T08:45:41Z
    @JsonProperty("updated_at")
    override val updatedAt: String?, // 2022-06-21T08:45:41Z
    @ApiModelProperty("Issue/Pull Request编号")
    val number: Long,
    @ApiModelProperty("Issue/Pull Request标题信息")
    val title: String,
    @ApiModelProperty("Issue/Pull Request创建用户")
    val user: GithubUser,
    @ApiModelProperty("Issue/Pull Request标签信息")
    val labels: List<GithubLabel>,
    @ApiModelProperty("issues/Pull Request 状态信息")
    val state: String,
    val locked: String,
    @ApiModelProperty("issues/Pull Request 受理人")
    val assignees: List<GithubUser>?,
    @JsonProperty("closed_at")
    val closedAt: String?,
    @JsonProperty("Issues/Pull Request 描述信息")
    val body: String?,
    @JsonProperty("pull_request")
    @ApiModelProperty("issues 关联的pull request信息，为空时代表仅在issue上操作")
    val pullRequest: GithubPullRequestUrl?,
    val milestone: GithubMilestone?
):GithubBaseInfo(
    id = id,
    url = url,
    htmlUrl = htmlUrl,
    nodeId = nodeId,
    updatedAt = updatedAt,
    createdAt = createdAt
)

data class GithubPullRequestUrl(
    val url: String,
    @JsonProperty("html_url")
    @ApiModelProperty("Pull Request链接[网页链接]")
    val htmlUrl: String,
    @JsonProperty("diff_url")
    @ApiModelProperty("Pull Request 修改内容链接[raw]")
    val diffUrl: String,
    @JsonProperty("patch_url")
    @ApiModelProperty("Pull Request 补丁链接[raw]")
    val patchUrl: String
)

@ApiModel("Github Issue 状态")
enum class GithubIssuesState(val value: String) {
    CLOSED("close"),
    OPEN("open")
}

@ApiModel("Github Issue 操作")
enum class GithubIssuesAction(val value: String) {
    @ApiModelProperty("重新打开")
    REOPENED("reopened"),

    @ApiModelProperty("关闭")
    CLOSED("closed"),

    @ApiModelProperty("创建")
    CREATED("created"),

    @ApiModelProperty("指派受理人")
    ASSIGNED("assigned"),

    @ApiModelProperty("标记")
    LABELED("labeled"),

    @ApiModelProperty("修改")
    EDITED("edited"),
}
