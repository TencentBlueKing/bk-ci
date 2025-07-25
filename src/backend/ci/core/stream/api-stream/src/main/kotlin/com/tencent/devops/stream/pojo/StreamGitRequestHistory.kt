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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "stream 历史构建模型-对应history页面")
data class StreamGitRequestHistory(
    @get:Schema(title = "ID")
    var id: Long?,
    @get:Schema(title = "OBJECT_KIND")
    val objectKind: String,
    @get:Schema(title = "OPERATION_KIND")
    val operationKind: String?,
    @get:Schema(title = "GIT_PROJECT_ID")
    val gitProjectId: Long,
    @get:Schema(title = "BRANCH")
    val branch: String,
    @get:Schema(title = "COMMIT_ID")
    val commitId: String,
    @get:Schema(title = "COMMIT_MESSAGE")
    val commitMsg: String?,
    @get:Schema(title = "COMMIT_TIMESTAMP")
    val commitTimeStamp: String?,
    @get:Schema(title = "用户")
    val userId: String,
    @get:Schema(title = "TOTAL_COMMIT_COUNT")
    val totalCommitCount: Long,
    @get:Schema(title = "MR_TITLE")
    var mrTitle: String?,
    @get:Schema(title = "MERGE_REQUEST_ID")
    val mergeRequestId: Long?,
    @get:Schema(title = "TARGET_BRANCH")
    val targetBranch: String?,
    @get:Schema(title = "DESCRIPTION")
    var description: String?,
    @get:Schema(title = "历史构建模型", required = false)
    val buildRecords: MutableList<StreamBuildHistory>
)
