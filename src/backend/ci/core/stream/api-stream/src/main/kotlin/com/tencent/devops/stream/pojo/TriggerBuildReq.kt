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

@Schema(title = "TriggerBuild请求")
data class TriggerBuildReq(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "分支")
    val branch: String,
    @get:Schema(title = "Custom commit message")
    val customCommitMsg: String?,
    @get:Schema(title = "yaml")
    val yaml: String?,
    @get:Schema(title = "描述")
    val description: String?,
    @get:Schema(title = "用户选择的触发CommitId")
    val commitId: String? = null,
    @get:Schema(title = "事件请求体")
    val payload: String? = null,
    @get:Schema(title = "模拟代码事件类型")
    val eventType: String? = null,
    @get:Schema(title = "手动触发输入参数")
    val inputs: Map<String, String>? = null,
    @get:Schema(title = "是否为子流水线触发")
    val subPipelineTriggerId: String? = null
)
