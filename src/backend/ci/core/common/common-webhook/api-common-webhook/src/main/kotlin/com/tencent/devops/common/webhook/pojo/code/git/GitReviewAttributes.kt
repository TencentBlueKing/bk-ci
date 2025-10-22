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

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitReviewAttributes(
    val id: Long,
    @JsonProperty("source_commit")
    val sourceCommit: String,
    @JsonProperty("source_branch")
    val sourceBranch: String,
    @JsonProperty("source_project_id")
    val sourceProjectId: String,
    @JsonProperty("target_commit")
    val targetCommit: String,
    @JsonProperty("target_branch")
    val targetBranch: String,
    @JsonProperty("target_project_id")
    val targetProjectId: String,
    @JsonProperty("author_id")
    val authorId: String?,
    @JsonProperty("assignee_id")
    val assigneeId: String?,
    val title: String,
    @JsonProperty("commit_check_state")
    val commitCheckState: String?,
    @JsonProperty("updated_by_id")
    val updatedById: String?,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("updated_at")
    val updatedAt: String,
    val state: String,
    val iid: Long,
    val description: String,
    val source: GitProject,
    val target: GitProject,
    @JsonProperty("last_commit")
    val lastCommit: GitCommit
)
