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

package com.tencent.devops.repository.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.repository.sdk.github.pojo.CheckRunOutput
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "check run 模型")
data class GithubCheckRuns(
    @get:Schema(title = "名称")
    val name: String,
    @JsonProperty("head_sha")
    @get:Schema(title = "head sha值", description = "head_sha")
    val headSha: String,
    @JsonProperty("details_url")
    @get:Schema(title = "详情链接", description = "details_url")
    val detailsUrl: String,
    @JsonProperty("external_id")
    @get:Schema(title = "拓展ID", description = "external_id")
    val externalId: String,
    @get:Schema(title = "状态")
    val status: String,
    @JsonProperty("started_at")
    @get:Schema(title = "开始于", description = "started_at")
    val startedAt: String?,
    @get:Schema(title = "结论")
    val conclusion: String?,
    @JsonProperty("completed_at")
    @get:Schema(title = "完成于", description = "completed_at")
    val completedAt: String?,
    @Parameter(description = "output", required = true)
    val output: CheckRunOutput? = null
)
