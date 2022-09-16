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

package com.tencent.devops.stream.v1.pojo

import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitIssueEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitNoteEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitReviewEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

// 该类提供给前端页面使用
@ApiModel("工蜂触发请求Req")
data class V1GitRequestEventReq(
    @ApiModelProperty("ID")
    var id: Long?,
    // TODO: 开源版时将不同仓库的事件类型使用统一的Stream Action做映射来判断，存储。
    @ApiModelProperty("事件类型")
    val objectKind: String,
    @ApiModelProperty("操作类型")
    val operationKind: String?,
    // 对于Push是action对于Mr是extension
    @ApiModelProperty("拓展操作")
    val extensionAction: String?,
    @ApiModelProperty("工蜂项目ID")
    val gitProjectId: Long,
    @ApiModelProperty("源工蜂项目ID")
    val sourceGitProjectId: Long?,
    @ApiModelProperty("分支名")
    val branch: String,
    @ApiModelProperty("目标分支名")
    val targetBranch: String?,
    @ApiModelProperty("提交ID")
    val commitId: String,
    @ApiModelProperty("提交说明")
    val commitMsg: String?,
    @ApiModelProperty("提交时间")
    val commitTimeStamp: String?,
    // 目前只在上下文中传递，后续看需求是否保存至数据库
    @ApiModelProperty("提交用户")
    val commitAuthorName: String?,
    @ApiModelProperty("用户")
    val userId: String,
    @ApiModelProperty("提交总数")
    val totalCommitCount: Long,
    // todo: 这里保存的是MR 的 iid 不是 mrId
    @ApiModelProperty("合并请求ID")
    val mergeRequestId: Long?,
    @ApiModelProperty("描述（已废弃）")
    var description: String?,
    @ApiModelProperty("合并请求标题")
    var mrTitle: String?,
    // TODO: 后续修改统一参数时可以将GitEvent统一放在这里维护
    @ApiModelProperty("Git事件对象")
    var gitEvent: GitEvent?,
    @ApiModelProperty("是否是删除分支触发")
    val deleteBranch: Boolean,
    @ApiModelProperty("是否是删除Tag触发")
    val deleteTag: Boolean,
    @ApiModelProperty("评论Id")
    var noteId: Long?,
    @ApiModelProperty("评论连接")
    var jumpUrl: String?,
    @ApiModelProperty("构建标题")
    var buildTitle: String?,
    @ApiModelProperty("构建跳转显示信息")
    var buildSource: String?
) {
    constructor(gitRequestEvent: V1GitRequestEvent) : this(
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
        buildSource = null
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
                    buildSource = commitId.take(9)
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
            else -> {
                when (objectKind) {
                    StreamGitObjectKind.SCHEDULE.value -> {
                        buildSource = commitId.take(9)
                    }
                    else -> {}
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
