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
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
data class GitReviewEvent(
    @JsonProperty("object_kind")
    val objectKind: String? = null,
    val id: String,
    val iid: String,
    val event: String,
    val author: GitUser,
    val reviewer: Reviewer? = null,
    val reviewers: List<Reviewer>,
    val repository: GitCommitRepository,
    @JsonProperty("project_id")
    val projectId: Long,
    @JsonProperty("author_id")
    val authorId: Long,
    @JsonProperty("reviewable_id")
    val reviewableId: Long? = null,
    @JsonProperty("reviewable_type")
    val reviewableType: String? = null,
    @JsonProperty("commit_id")
    val commitId: String? = null,
    val state: String,
    @JsonProperty("restrict_type")
    val restrictType: String? = null,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("updated_at")
    val updatedAt: String
) : GitEvent() {
    companion object {
        const val classType = "review"
        const val ACTION_APPROVED = "approved"
        const val ACTION_APPROVING = "approving"
        const val ACTION_CLOSE = "closed"
        const val ACTION_CHANGE_DENIED = "change_denied"
        const val ACTION_CHANGE_REQUIRED = "change_required"
        const val ACTION_EMPTY = "empty"
    }
}

data class Reviewer(
    val reviewer: GitUser,
    val id: Long,
    @JsonProperty("review_id")
    val reviewId: Long,
    @JsonProperty("user_id")
    val userId: Long,
    @JsonProperty("project_id")
    val projectId: Long,
    val type: String,
    val state: String
)
