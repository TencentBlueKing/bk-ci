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
 *
 */

package com.tencent.devops.common.sdk.github.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.github.pojo.GithubCommitFile

data class CompareTwoCommitsResponse(
    @JsonProperty("ahead_by")
    val aheadBy: Int,
    @JsonProperty("base_commit")
    val baseCommit: CommitResponse,
    @JsonProperty("behind_by")
    val behindBy: Int,
    @JsonProperty("commits")
    val commits: List<CommitResponse>,
    @JsonProperty("diff_url")
    val diffUrl: String,
    @JsonProperty("files")
    val files: List<GithubCommitFile>,
    @JsonProperty("html_url")
    val htmlUrl: String,
    @JsonProperty("merge_base_commit")
    val mergeBaseCommit: CommitResponse,
    @JsonProperty("patch_url")
    val patchUrl: String,
    @JsonProperty("permalink_url")
    val permalinkUrl: String,
    @JsonProperty("status")
    val status: String,
    @JsonProperty("total_commits")
    val totalCommits: Int,
    @JsonProperty("url")
    val url: String
)
