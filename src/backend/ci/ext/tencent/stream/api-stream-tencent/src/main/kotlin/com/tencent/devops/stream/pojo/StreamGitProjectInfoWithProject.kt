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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "stream 部分项目信息附带蓝盾项目信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class StreamGitProjectInfoWithProject(
    @get:Schema(title = "项目ID", description = "id")
    @JsonProperty("id")
    val gitProjectId: Long,
    @get:Schema(title = "项目名称", description = "name")
    @JsonProperty("name")
    val name: String,
    @get:Schema(title = "页面地址", description = "web_url")
    @JsonProperty("web_url")
    val homepage: String?,
    @get:Schema(title = "HTTP链接", required = true, description = "http_url_to_repo")
    @JsonProperty("http_url_to_repo")
    val gitHttpUrl: String,
    @get:Schema(title = "HTTPS链接", description = "https_url_to_repo")
    @JsonProperty("https_url_to_repo")
    val gitHttpsUrl: String?,
    @get:Schema(title = "gitSshUrl", description = "ssh_url_to_repo")
    @JsonProperty("ssh_url_to_repo")
    val gitSshUrl: String?,
    @get:Schema(title = "带有名空间的项目名称", description = "name_with_namespace")
    @JsonProperty("name_with_namespace")
    val nameWithNamespace: String,
    @get:Schema(title = "带有名空间的项目路径", description = "path_with_namespace")
    @JsonProperty("path_with_namespace")
    val pathWithNamespace: String?,
    @get:Schema(title = "项目的默认分支", description = "default_branch")
    @JsonProperty("default_branch")
    val defaultBranch: String?,
    @get:Schema(title = "项目的描述信息", description = "description")
    @JsonProperty("description")
    val description: String?,
    @get:Schema(title = "项目的头像信息", description = "avatar_url")
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    @get:Schema(title = "环境路由", description = "routerTag")
    @JsonProperty("routerTag")
    val routerTag: String?,
    @get:Schema(title = "带前缀项目ID", description = "projectCode")
    @JsonProperty("projectCode")
    val projectCode: String? = null
)
