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
import com.tencent.devops.common.webhook.enums.code.github.GithubPushOperationKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import io.swagger.v3.oas.annotations.media.Schema

// 将git event统一处理方便前端展示
@Schema(title = "stream 触发请求")
data class GitRequestEvent(
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
    // 这里保存的是MR 的 iid 不是 mrId
    @get:Schema(title = "合并请求ID")
    val mergeRequestId: Long?,
    @get:Schema(title = "事件原文")
    val event: String,
    @get:Schema(title = "描述（已废弃）")
    var description: String?,
    @get:Schema(title = "合并请求标题")
    var mrTitle: String?,
    @get:Schema(title = "Git事件对象")
    var gitEvent: CodeWebhookEvent?,
    @get:Schema(title = "去掉头部url的homepage")
    var gitProjectName: String?,
    @get:Schema(title = "远程仓库触发时得到的主库流水线列表")
    var repoTriggerPipelineList: List<StreamRepoHookEvent>? = null,
    @get:Schema(title = "变更的yaml文件")
    var changeYamlList: List<ChangeYamlList> = emptyList()
) {
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

fun GitRequestEvent.isMr() = objectKind == StreamGitObjectKind.MERGE_REQUEST.value

fun GitRequestEvent.isFork(): Boolean {
    return (objectKind == StreamGitObjectKind.MERGE_REQUEST.value ||
        objectKind == StreamGitObjectKind.PULL_REQUEST.value) &&
        sourceGitProjectId != null &&
        sourceGitProjectId != gitProjectId
}

/**
 * 判断是否是删除分支的event这个Event不做构建只做删除逻辑
 */
fun GitRequestEvent.isDeleteBranch() = checkGithubDeleteBranch() || checkTGitDeleteBranch()

fun GitRequestEvent.isDeleteTag() = checkGithubDeleteTag() || checkTGitDeleteTag()

fun GitRequestEvent.checkTGitDeleteBranch() = objectKind == StreamGitObjectKind.PUSH.value &&
    operationKind == TGitPushOperationKind.DELETE.value &&
    (
        extensionAction == TGitPushActionKind.DELETE_BRANCH.value ||
            commitId == GitRequestEvent.DELETE_BRANCH_COMMITID_FROM_CLIENT
        )

fun GitRequestEvent.checkGithubDeleteBranch() = objectKind == StreamGitObjectKind.PUSH.value &&
    operationKind == GithubPushOperationKind.DELETE.value &&
    commitId == GitRequestEvent.DELETE_BRANCH_COMMITID_FROM_CLIENT

fun GitRequestEvent.checkTGitDeleteTag(): Boolean {
    return objectKind == StreamGitObjectKind.TAG_PUSH.value &&
        operationKind == TGitPushOperationKind.DELETE.value
}

fun GitRequestEvent.checkGithubDeleteTag(): Boolean {
    return objectKind == StreamGitObjectKind.TAG_PUSH.value &&
        operationKind == GithubPushOperationKind.DELETE.value
}
