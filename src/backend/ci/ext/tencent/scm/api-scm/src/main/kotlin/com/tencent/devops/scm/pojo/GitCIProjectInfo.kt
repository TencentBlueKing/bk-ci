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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂CI查询代码库项目信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitCIProjectInfo(
    @ApiModelProperty("项目ID")
    @JsonProperty("id")
    val gitProjectId: Int,
    @ApiModelProperty("项目名称")
    @JsonProperty("name")
    val name: String,
    @ApiModelProperty("页面地址")
    @JsonProperty("web_url")
    val homepage: String?,
    @ApiModelProperty("HTTP链接", required = true)
    @JsonProperty("http_url_to_repo")
    val gitHttpUrl: String,
    @ApiModelProperty("HTTPS链接")
    @JsonProperty("https_url_to_repo")
    val gitHttpsUrl: String?,
    @ApiModelProperty("gitSshUrl")
    @JsonProperty("ssh_url_to_repo")
    val gitSshUrl: String?,
    @ApiModelProperty("带有所有者的项目名称")
    @JsonProperty("name_with_namespace")
    val nameWithNamespace: String,
    @ApiModelProperty("带有所有者的项目路径")
    @JsonProperty("path_with_namespace")
    val pathWithNamespace: String?,
    @ApiModelProperty("项目的默认分支")
    @JsonProperty("default_branch")
    val defaultBranch: String?,
    @ApiModelProperty("项目的描述信息")
    @JsonProperty("description")
    val description: String?,
    @ApiModelProperty("项目的头像信息")
    @JsonProperty("avatar_url")
    val avatarUrl: String?
)
