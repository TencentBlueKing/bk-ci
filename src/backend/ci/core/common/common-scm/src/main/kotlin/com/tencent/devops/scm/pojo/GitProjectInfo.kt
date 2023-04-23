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
 */

package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("git项目信息")
data class GitProjectInfo(
    @ApiModelProperty("项目ID", name = "id")
    @JsonProperty("id")
    val id: Long,
    @ApiModelProperty("项目名称", name = "name")
    @JsonProperty("name")
    val name: String,
    @ApiModelProperty("命名空间名称", name = "name_with_namespace")
    @JsonProperty("name_with_namespace")
    val namespaceName: String,
    @ApiModelProperty("可见范围", name = "visibility_level")
    @JsonProperty("visibility_level")
    val visibilityLevel: Int?,
    @ApiModelProperty("仓库地址", required = true, name = "http_url_to_repo")
    @JsonProperty("http_url_to_repo")
    val repositoryUrl: String,
    @ApiModelProperty("页面地址", name = "web_url")
    @JsonProperty("web_url")
    val homepage: String?,
    @ApiModelProperty("HTTPS链接", name = "https_url_to_repo")
    @JsonProperty("https_url_to_repo")
    val gitHttpsUrl: String?,
    @ApiModelProperty("gitSshUrl", name = "ssh_url_to_repo")
    @JsonProperty("ssh_url_to_repo")
    val gitSshUrl: String?,
    @ApiModelProperty("带有名空间的项目路径", name = "path_with_namespace")
    @JsonProperty("path_with_namespace")
    val pathWithNamespace: String?,
    @ApiModelProperty("项目的默认分支", name = "default_branch")
    @JsonProperty("default_branch")
    val defaultBranch: String?,
    @ApiModelProperty("项目的描述信息", name = "description")
    @JsonProperty("description")
    val description: String?,
    @ApiModelProperty("项目的头像信息", name = "avatar_url")
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    @ApiModelProperty("项目创建时间", name = "created_at")
    @JsonProperty("created_at")
    val createdAt: String?,
    @ApiModelProperty("项目创建人id", name = "creator_id")
    @JsonProperty("creator_id")
    val creatorId: String?
)
