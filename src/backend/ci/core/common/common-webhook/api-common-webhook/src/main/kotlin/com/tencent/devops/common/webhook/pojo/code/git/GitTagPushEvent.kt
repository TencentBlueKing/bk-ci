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

package com.tencent.devops.common.webhook.pojo.code.git

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.webhook.enums.code.tgit.TGitTagPushActionKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitTagPushOperationKind

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitTagPushEvent(
    val before: String,
    val after: String,
    val action_kind: String?,
    val ref: String,
    val checkout_sha: String?,
    val user_name: String,
    val project_id: Long,
    val repository: GitCommitRepository,
    val commits: List<GitCommit>?,
    val total_commits_count: Int,
    val operation_kind: String?,
    val create_from: String? = null,
    @JsonProperty("push_timestamp")
    val pushTimestamp: String?,
    val message: String?
) : GitEvent() {
    companion object {
        const val classType = "tag_push"
    }
}

fun GitTagPushEvent.isDeleteTag(): Boolean {
    // 工蜂web端删除
    if (action_kind == TGitTagPushActionKind.DELETE_TAG.value) {
        return true
    }
    // 发送到工蜂的客户端删除
    if (action_kind == TGitTagPushActionKind.CLIENT_PUSH.value &&
        operation_kind == TGitTagPushOperationKind.DELETE.value &&
        after.filter { it != '0' }.isBlank()
    ) {
        return true
    }
    // 非工蜂代码库Tag删除
    if (action_kind == null && after.filter { it != '0' }.isBlank()) {
        return true
    }
    return false
}
