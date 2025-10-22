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

package com.tencent.devops.common.webhook.pojo.code.gitlab

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommitEvent(
    val object_kind: String,
    val before: String,
    val after: String,
    val ref: String,
    val checkout_sha: String,
    val user_name: String,
    val project_id: Long,
    val project: GitlabCommitProject,
    val commits: List<GitlabCommit>,
    val total_commits_count: Int,
    val repository: GitlabCommitRepository
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommitProject(
    val name: String,
    val web_url: String,
    val git_ssh_url: String,
    val git_http_url: String,
    val path_with_namespace: String,
    val url: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommit(
    val id: String,
    val message: String,
    val timestamp: String,
    val url: String,
    val author: GitlabCommitAuthor
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommitAuthor(
    val name: String,
    val email: String
)

@Suppress("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommitRepository(
    val name: String,
    val url: String,
    val homepage: String,
    val git_http_url: String,
    val git_ssh_url: String
)
