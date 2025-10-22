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

package com.tencent.devops.repository.pojo.git

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "更新git项目信息")
data class UpdateGitProjectInfo(
    @get:Schema(title = "项目名", description = "name")
    @JsonProperty("name")
    val name: String? = null,
    @get:Schema(title = "项目是否可以被fork", description = "fork_enabled")
    @JsonProperty("fork_enabled")
    val forkEnabled: Boolean? = null,
    @get:Schema(title = "项目描述", description = "description")
    @JsonProperty("description")
    val description: String? = null,
    @get:Schema(title = "项目默认分支", description = "default_branch")
    @JsonProperty("default_branch")
    val defaultBranch: String? = null,
    @get:Schema(title = "文件大小限制，单位:MB", description = "limit_file_size")
    @JsonProperty("limit_file_size")
    val limitFileSize: Float? = null,
    @get:Schema(title = "LFS文件大小限制，单位:MB", description = "limit_lfs_file_size")
    @JsonProperty("limit_lfs_file_size")
    val limitLfsFileSize: Float? = null,
    @get:Schema(title = "缺陷配置", description = "issues_enabled")
    @JsonProperty("issues_enabled")
    val issuesEnabled: Boolean? = null,
    @get:Schema(title = "合并请求配置", description = "merge_requests_enabled")
    @JsonProperty("merge_requests_enabled")
    val mergeRequestsEnabled: Boolean? = null,
    @get:Schema(title = "维基配置", description = "wiki_enabled")
    @JsonProperty("wiki_enabled")
    val wikiEnabled: Boolean? = null,
    @get:Schema(title = "评审配置", description = "review_enabled")
    @JsonProperty("review_enabled")
    val reviewEnabled: Boolean? = null,
    @get:Schema(title = "推送或创建tag规则", description = "tag_name_regex")
    @JsonProperty("tag_name_regex")
    val tagNameRegex: String? = null,
    @get:Schema(title = "推送或创建tag权限", description = "tag_create_push_level")
    @JsonProperty("tag_create_push_level")
    val tagCreatePushLevel: Int? = null,
    @get:Schema(title = "项目可视范围", description = "visibility_level")
    @JsonProperty("visibility_level")
    val visibilityLevel: Int? = null
)
