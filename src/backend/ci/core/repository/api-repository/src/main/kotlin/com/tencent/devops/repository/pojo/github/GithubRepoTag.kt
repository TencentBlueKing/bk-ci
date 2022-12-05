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

package com.tencent.devops.repository.pojo.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * github获取tag列表接口: /repos/{owner}/{repo}/tags
 * {
 "name": "v1.7.22-RC.2",
 "zipball_url": "https://api.github.com/repos/Tencent/bk-ci/zipball/refs/tags/v1.7.22-RC.2",
 "tarball_url": "https://api.github.com/repos/Tencent/bk-ci/tarball/refs/tags/v1.7.22-RC.2",
 "commit": {
 "sha": "c4a9464c710eae94c1538ede27e538c61643fe65",
 "url": "https://api.github.com/repos/Tencent/bk-ci/commits/c4a9464c710eae94c1538ede27e538c61643fe65"
 },
 "node_id": "MDM6UmVmMTg5MTUzNDkxOnJlZnMvdGFncy92MS43LjIyLVJDLjI="
 }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("仓库tag信息")
data class GithubRepoTag(
    @ApiModelProperty("名称")
    val name: String,
    @JsonProperty("zipball_url")
    val zipballUrl: String,
    @JsonProperty("tarball_url")
    val tarballUrl: String,
    val commit: GithubRepoCommit,
    @JsonProperty("node_id")
    val nodeId: String
)
