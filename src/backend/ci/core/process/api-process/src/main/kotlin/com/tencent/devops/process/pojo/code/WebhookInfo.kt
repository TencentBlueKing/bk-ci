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

package com.tencent.devops.process.pojo.code

import io.swagger.v3.oas.annotations.media.Schema

data class WebhookInfo(
    @get:Schema(title = "代码库类型", required = true)
    val codeType: String?, // CodeType.name
    @get:Schema(title = "代码库完整名称", required = true)
    val nameWithNamespace: String?,
    @get:Schema(title = "仓库url链接", required = false)
    val webhookRepoUrl: String?,
    @get:Schema(title = "分支名（目标分支）", required = false)
    val webhookBranch: String?,
    @get:Schema(title = "别名", required = false)
    val webhookAliasName: String?,
    @get:Schema(title = "webhook类型", required = false)
    val webhookType: String?,
    @get:Schema(title = "事件类型", required = false)
    val webhookEventType: String?,
    @get:Schema(title = "提交信息", required = false)
    val webhookMessage: String?,
    @get:Schema(title = "提交信息id", required = false)
    val webhookCommitId: String?,
    @get:Schema(title = "参考信息(commit_id,mr_id,tag,issue_id,review_id,note_id等)", required = true)
    val refId: String?,
    @get:Schema(title = "合并后commitId", required = false)
    // 合并后commitId
    val webhookMergeCommitSha: String?,
    @get:Schema(title = "源分支", required = false)
    // 源分支
    val webhookSourceBranch: String?,
    // mr id
    val mrId: String?,
    // mr iid
    val mrIid: String?,
    // mr url
    val mrUrl: String?,
    // webhook仓库授权用户
    val repoAuthUser: String?,
    // tag 名称
    val tagName: String?,
    // issue iid,
    val issueIid: String?,
    // note id
    val noteId: String?,
    // review id
    val reviewId: String?,
    // 父流水线项目ID
    val parentProjectId: String?,
    // 父流水线流水线ID
    val parentPipelineId: String?,
    // 父流水线名称
    val parentPipelineName: String?,
    // 父流水线buildId
    val parentBuildId: String?,
    // 父流水线构建号
    val parentBuildNum: String?,
    // 触发材料url
    val linkUrl: String?,
    // 自定义触发材料ID
    val materialId: String?,
    // 自定义触发材料名
    val materialName: String?
)
