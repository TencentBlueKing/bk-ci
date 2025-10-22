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
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMergeActionKind

@Suppress("ALL")
data class GitMergeRequestEvent(
    val user: GitUser,
    val manual_unlock: Boolean? = false,
    val object_attributes: GitMRAttributes
) : GitEvent() {

    // 新建
    fun isCreated() = object_attributes.action == ACTION_CREATED

    // 源分支更新
    fun isUpdate() = object_attributes.action == ACTION_UPDATED &&
            object_attributes.extension_action == "push-update"

    // MR基本信息更新
    fun isUpdateInfo() = object_attributes.action == ACTION_UPDATED

    // 合并
    fun isClosed() = object_attributes.action == ACTION_CLOSED

    // 重新打开
    fun isReopen() = object_attributes.action == ACTION_REOPENED

    // 合并
    fun isMerged() = object_attributes.action == ACTION_MERGED

    companion object {
        const val classType = "merge_request"
        const val ACTION_CREATED = "open"
        const val ACTION_UPDATED = "update"
        const val ACTION_CLOSED = "close"
        const val ACTION_REOPENED = "reopen"
        const val ACTION_MERGED = "merge"
    }
}

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitMRAttributes(
    val id: Long,
    val target_branch: String,
    val source_branch: String,
    val author_id: Long,
    val assignee_id: Long,
    val title: String,
    val created_at: String,
    val updated_at: String,
    val state: String,
    val merge_status: String,
    val target_project_id: Long,
    val source_project_id: Long,
    val iid: Long,
    val description: String?,
    val source: GitProject,
    val target: GitProject,
    val last_commit: GitCommit,
    val url: String?,
    val action: String?,
    val extension_action: String?,
    @JsonProperty("merge_type")
    val mergeType: String? = null,
    @JsonProperty("merge_commit_sha")
    val mergeCommitSha: String? = null
)

fun GitMergeRequestEvent.isMrMergeEvent() = this.object_attributes.action == TGitMergeActionKind.MERGE.value

fun GitMergeRequestEvent.isMrForkEvent() =
    this.object_attributes.target_project_id != this.object_attributes.source_project_id
