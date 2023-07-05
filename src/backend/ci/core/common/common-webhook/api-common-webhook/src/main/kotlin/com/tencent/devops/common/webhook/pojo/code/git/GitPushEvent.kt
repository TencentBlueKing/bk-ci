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

package com.tencent.devops.common.webhook.pojo.code.git

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitPushEvent(
    val before: String,
    val after: String,
    val ref: String,
    val checkout_sha: String?,
    val user_name: String,
    val project_id: Long,
    val repository: GitCommitRepository,
    val commits: List<GitCommit>?,
    val total_commits_count: Int,
    val operation_kind: String?,
    val action_kind: String?,
    val push_options: Map<String, String>?,
    val create_and_update: Boolean?,
    @JsonProperty("diff_files")
    val diffFiles: List<GitDiffFile>?
) : GitEvent() {
    companion object {
        const val classType = "push"
    }
}

fun GitPushEvent.isDeleteBranch(): Boolean {
    // 工蜂web端删除
    if (action_kind == TGitPushActionKind.DELETE_BRANCH.value) {
        return true
    }
    // 发送到工蜂的客户端删除
    if (action_kind == TGitPushActionKind.CLIENT_PUSH.value &&
        operation_kind == TGitPushOperationKind.DELETE.value &&
        after.filter { it != '0' }.isBlank()
    ) {
        return true
    }
    return false
}
