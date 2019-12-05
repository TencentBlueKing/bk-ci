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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.repository.pojo.git

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("更新git项目信息")
data class UpdateGitProjectInfo(
    @ApiModelProperty("项目名")
    @JsonProperty("name")
    val name: String? = null,
    @ApiModelProperty("项目是否可以被fork")
    @JsonProperty("fork_enabled")
    val forkEnabled: Boolean? = null,
    @ApiModelProperty("项目描述")
    @JsonProperty("description")
    val description: String? = null,
    @ApiModelProperty("项目默认分支")
    @JsonProperty("default_branch")
    val defaultBranch: String? = null,
    @ApiModelProperty("文件大小限制，单位:MB")
    @JsonProperty("limit_file_size")
    val limitFileSize: Float? = null,
    @ApiModelProperty("LFS文件大小限制，单位:MB")
    @JsonProperty("limit_lfs_file_size")
    val limitLfsFileSize: Float? = null,
    @ApiModelProperty("缺陷配置")
    @JsonProperty("issues_enabled")
    val issuesEnabled: Boolean? = null,
    @ApiModelProperty("合并请求配置")
    @JsonProperty("merge_requests_enabled")
    val mergeRequestsEnabled: Boolean? = null,
    @ApiModelProperty("维基配置")
    @JsonProperty("wiki_enabled")
    val wikiEnabled: Boolean? = null,
    @ApiModelProperty("评审配置")
    @JsonProperty("review_enabled")
    val reviewEnabled: Boolean? = null,
    @ApiModelProperty("推送或创建tag规则")
    @JsonProperty("tag_name_regex")
    val tagNameRegex: String? = null,
    @ApiModelProperty("推送或创建tag权限")
    @JsonProperty("tag_create_push_level")
    val tagCreatePushLevel: Int? = null,
    @ApiModelProperty("项目可视范围")
    @JsonProperty("visibility_level")
    val visibilityLevel: Int? = null
)