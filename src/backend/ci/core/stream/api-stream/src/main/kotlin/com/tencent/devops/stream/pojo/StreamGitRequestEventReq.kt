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

package com.tencent.devops.stream.pojo

import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import io.swagger.v3.oas.annotations.media.Schema

// 该类提供给前端页面使用
@Schema(title = "stream 触发请求Req")
data class StreamGitRequestEventReq(
    @get:Schema(title = "ID")
    var id: Long?,
    @get:Schema(title = "事件类型")
    val objectKind: String,
    @get:Schema(title = "操作类型")
    val operationKind: String?,
    // 对于Push是action对于Mr是extension
    @get:Schema(title = "拓展操作")
    val extensionAction: String?,
    @get:Schema(title = "stream 项目ID")
    val gitProjectId: Long,
    @get:Schema(title = "源stream 项目ID")
    val sourceGitProjectId: Long?,
    @get:Schema(title = "分支名")
    val branch: String,
    @get:Schema(title = "目标分支名")
    val targetBranch: String?,
    @get:Schema(title = "提交ID")
    val commitId: String,
    @get:Schema(title = "提交说明")
    val commitMsg: String?,
    @get:Schema(title = "提交时间")
    val commitTimeStamp: String?,
    // 目前只在上下文中传递，后续看需求是否保存至数据库
    @get:Schema(title = "提交用户")
    val commitAuthorName: String?,
    @get:Schema(title = "用户")
    val userId: String,
    @get:Schema(title = "提交总数")
    val totalCommitCount: Long,
    @get:Schema(title = "合并请求ID")
    val mergeRequestId: Long?,
    @get:Schema(title = "描述（已废弃）")
    var description: String?,
    @get:Schema(title = "合并请求标题")
    var mrTitle: String?,
    @get:Schema(title = "Git事件对象")
    var gitEvent: GitEvent?,
    @get:Schema(title = "是否是删除分支触发")
    val deleteBranch: Boolean,
    @get:Schema(title = "是否是删除Tag触发")
    val deleteTag: Boolean,
    @get:Schema(title = "评论Id")
    var noteId: Long?,
    @get:Schema(title = "评论连接")
    var jumpUrl: String?,
    @get:Schema(title = "构建标题")
    var buildTitle: String?,
    @get:Schema(title = "构建跳转显示信息")
    var buildSource: String?,
    @get:Schema(title = "变更的yaml文件")
    var changeYamlList: List<ChangeYamlList> = emptyList()
) {
    constructor(gitRequestEvent: GitRequestEvent, homepage: String) : this(
        id = gitRequestEvent.id,
        objectKind = gitRequestEvent.objectKind,
        operationKind = gitRequestEvent.operationKind,
        extensionAction = gitRequestEvent.extensionAction,
        gitProjectId = gitRequestEvent.gitProjectId,
        sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
        branch = gitRequestEvent.branch,
        targetBranch = gitRequestEvent.targetBranch,
        commitId = gitRequestEvent.commitId,
        commitMsg = gitRequestEvent.commitMsg,
        commitTimeStamp = gitRequestEvent.commitTimeStamp,
        commitAuthorName = gitRequestEvent.commitAuthorName,
        userId = gitRequestEvent.userId,
        totalCommitCount = gitRequestEvent.totalCommitCount,
        mergeRequestId = gitRequestEvent.mergeRequestId,
        description = gitRequestEvent.description,
        mrTitle = gitRequestEvent.mrTitle,
        gitEvent = null,
        deleteBranch = gitRequestEvent.isDeleteBranch(),
        deleteTag = gitRequestEvent.isDeleteTag(),
        noteId = null,
        jumpUrl = null,
        buildTitle = null,
        buildSource = null,
        changeYamlList = gitRequestEvent.changeYamlList
    ) {
        // 组装信息：用于传给前端页面使用
        when (gitRequestEvent.gitEvent) {
            is GitNoteEvent -> {
                val event = gitRequestEvent.gitEvent as GitNoteEvent
                noteId = event.objectAttributes.id
                jumpUrl = event.objectAttributes.url
                buildTitle = commitMsg
                buildSource = "[$noteId]"
            }
            is GitPushEvent -> {
                val event = gitRequestEvent.gitEvent as GitPushEvent
                if (deleteBranch) {
                    buildTitle = "Branch $branch deleted by $userId"
                } else {
                    jumpUrl = event.repository.homepage + "/commit/" + gitRequestEvent.commitId
                    buildTitle = commitMsg
                    buildSource = commitId.take(8)
                }
            }
            is GitTagPushEvent -> {
                val event = gitRequestEvent.gitEvent as GitTagPushEvent
                if (deleteTag) {
                    buildTitle = "Tag $branch deleted by $userId"
                } else {
                    jumpUrl = event.repository.homepage + "/-/tags/" + gitRequestEvent.branch
                    buildTitle = commitMsg
                    buildSource = branch
                }
            }
            is GitMergeRequestEvent -> {
                val event = gitRequestEvent.gitEvent as GitMergeRequestEvent
                jumpUrl = event.object_attributes.target.web_url + "/merge_requests/" + gitRequestEvent.mergeRequestId
                buildTitle = mrTitle
                buildSource = "[!$mergeRequestId]"
            }
            is GitIssueEvent -> {
                val event = gitRequestEvent.gitEvent as GitIssueEvent
                jumpUrl = event.repository.homepage + "/issues/" + gitRequestEvent.mergeRequestId
                buildTitle = commitMsg
                buildSource = "[$mergeRequestId]"
            }
            is GitReviewEvent -> {
                val event = gitRequestEvent.gitEvent as GitReviewEvent
                jumpUrl = event.repository.homepage + "/reviews/" + gitRequestEvent.mergeRequestId
                buildTitle = commitMsg
                buildSource = "[$mergeRequestId]"
            }
            is GithubPullRequestEvent -> {
                val event = gitRequestEvent.gitEvent as GithubPullRequestEvent
                jumpUrl = event.pullRequest.htmlUrl
                buildTitle = mrTitle
                buildSource = "[!$mergeRequestId]"
            }
            is GithubPushEvent -> {
                val event = gitRequestEvent.gitEvent as GithubPushEvent
                when {
                    event.ref.startsWith("refs/heads/") -> {
                        if (deleteBranch) {
                            buildTitle = "Branch $branch deleted by $userId"
                        } else {
                            jumpUrl = event.headCommit?.url
                            buildTitle = commitMsg
                            buildSource = commitId.take(9)
                        }
                    }
                    event.ref.startsWith("refs/tags/") -> {
                        if (deleteTag) {
                            buildTitle = "Tag $branch deleted by $userId"
                        } else {
                            jumpUrl = event.repository.url + "/releases/tag/" + branch
                            buildTitle = commitMsg
                            buildSource = branch
                        }
                    }
                }
            }
            else -> {
                when (objectKind) {
                    StreamGitObjectKind.SCHEDULE.value,
                    StreamGitObjectKind.OPENAPI.value,
                    StreamGitObjectKind.MANUAL.value -> {
                        buildSource = commitId.take(8)
                        jumpUrl = homepage + "/commit/" + gitRequestEvent.commitId
                    }
                    else -> {
                    }
                }
                // 兼容给list接口有title数据。list接口并没有生成大对象，减少负担
                when (operationKind) {
                    TGitPushOperationKind.DELETE.value -> {
                        if (objectKind == StreamGitObjectKind.PUSH.value) {
                            buildTitle = "Branch $branch deleted by $userId"
                        }
                        if (objectKind == StreamGitObjectKind.TAG_PUSH.value) {
                            buildTitle = "Tag $branch deleted by $userId"
                        }
                    }
                    else -> {
                        buildTitle = commitMsg
                    }
                }
            }
        }
    }
}
