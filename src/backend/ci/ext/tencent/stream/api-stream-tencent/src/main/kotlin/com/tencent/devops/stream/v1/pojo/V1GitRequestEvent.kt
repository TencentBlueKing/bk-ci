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
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.stream.pojo.GitRequestEvent
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

// 工蜂所有推过来的请求
@ApiModel("工蜂触发请求")
data class V1GitRequestEvent(
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
    @ApiModelProperty("事件原文")
    val event: String,
    @ApiModelProperty("描述（已废弃）")
    var description: String?,
    @ApiModelProperty("合并请求标题")
    var mrTitle: String?,
    // TODO: 后续修改统一参数时可以将GitEvent统一放在这里维护
    @ApiModelProperty("Git事件对象")
    var gitEvent: CodeWebhookEvent?,
    @ApiModelProperty("去掉头部url的homepage")
    var gitProjectName: String?,
    @ApiModelProperty("远程仓库触发时得到的主库流水线列表")
    var repoTriggerPipelineList: List<V1StreamRepoHookEvent>? = null
) {
    constructor(r: GitRequestEvent) : this(
        id = r.id,
        objectKind = r.objectKind,
        operationKind = r.operationKind,
        extensionAction = r.extensionAction,
        gitProjectId = r.gitProjectId,
        sourceGitProjectId = r.sourceGitProjectId,
        branch = r.branch,
        targetBranch = r.targetBranch,
        commitId = r.commitId,
        commitMsg = r.commitMsg,
        commitTimeStamp = r.commitTimeStamp,
        commitAuthorName = r.commitAuthorName,
        userId = r.userId,
        totalCommitCount = r.totalCommitCount,
        mergeRequestId = r.mergeRequestId,
        event = r.event,
        description = r.description,
        mrTitle = r.mrTitle,
        gitEvent = r.gitEvent,
        gitProjectName = r.gitProjectName,
        repoTriggerPipelineList = r.repoTriggerPipelineList?.map {
            V1StreamRepoHookEvent(
                pipelineId = it.pipelineId,
                sourceGitProjectPath = it.sourceGitProjectPath,
                targetGitProjectId = it.targetGitProjectId
            )
        }
    )

    companion object {
        // 对应client下删除分支的场景，after=0000000000000000000000000000000000000000，表示删除分支。
        const val DELETE_BRANCH_COMMITID_FROM_CLIENT = "0000000000000000000000000000000000000000"
    }

    // 获取fork库的项目id
    fun getForkGitProjectId(): Long? {
        return if (isFork() && sourceGitProjectId != gitProjectId) {
            sourceGitProjectId!!
        } else {
            null
        }
    }

    // 当人工触发时不推送CommitCheck消息
    fun sendCommitCheck() = objectKind != StreamGitObjectKind.MANUAL.value
}

fun V1GitRequestEvent.isMr() = objectKind == StreamGitObjectKind.MERGE_REQUEST.value

fun V1GitRequestEvent.isFork(): Boolean {
    return objectKind == StreamGitObjectKind.MERGE_REQUEST.value &&
        sourceGitProjectId != null &&
        sourceGitProjectId != gitProjectId
}

/**
 * 判断是否是删除分支的event这个Event不做构建只做删除逻辑
 */
fun V1GitRequestEvent.isDeleteBranch(): Boolean {
    return objectKind == StreamGitObjectKind.PUSH.value &&
        operationKind == TGitPushOperationKind.DELETE.value &&
        (
            extensionAction == TGitPushActionKind.DELETE_BRANCH.value ||
                commitId == V1GitRequestEvent.DELETE_BRANCH_COMMITID_FROM_CLIENT
            )
}

fun V1GitRequestEvent.isDeleteTag(): Boolean {
    return objectKind == StreamGitObjectKind.TAG_PUSH.value &&
        operationKind == TGitPushOperationKind.DELETE.value
}
