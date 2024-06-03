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
import io.swagger.v3.oas.annotations.media.Schema

/**
 * {
"ref": "refs/tags/test_v1",
"node_id": "MDM6UmVmNjEyOTgzNDU6dGVzdF92MQ==",
"url": "https://api.github.com/repos/xxx/goroutine/git/refs/tags/test_v1",
"object": {
"sha": "bdd43327c549105f5e1296d65121afbeb0f3f1ef",
"type": "commit",
"url": "https://api.github.com/repos/xxx/goroutine/git/commits/bdd43327c549105f5e1296d65121afbeb0f3f1ef"
}
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "获取tag返回模型")
data class GithubTag(
    @get:Schema(title = "远程引用")
    val ref: String,
    @JsonProperty("node_id")
    @get:Schema(title = "节点id", description = "node_id")
    val nodeId: String,
    @get:Schema(title = "url地址")
    val url: String,
    @get:Schema(title = "tag数据", description = "object")
    @JsonProperty("object")
    val tagObject: GithubObject?
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "")
data class GithubObject(
    @get:Schema(title = "sha值")
    val sha: String,
    @get:Schema(title = "类型")
    val type: String,
    @get:Schema(title = "url地址")
    val url: String
)
