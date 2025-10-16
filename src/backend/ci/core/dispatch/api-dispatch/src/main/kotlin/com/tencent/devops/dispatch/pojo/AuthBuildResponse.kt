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

package com.tencent.devops.dispatch.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "外部鉴权响应")
data class AuthBuildResponse(
    @get:Schema(title = "鉴权是否成功", required = true)
    val success: Boolean,

    @get:Schema(title = "项目ID", required = false)
    val projectId: String? = null,

    @get:Schema(title = "流水线ID", required = false)
    val pipelineId: String? = null,

    @get:Schema(title = "构建ID", required = false)
    val buildId: String? = null,

    @get:Schema(title = "Agent ID", required = false)
    val agentId: String? = null,

    @get:Schema(title = "VM序列ID", required = false)
    val vmSeqId: String? = null,

    @get:Schema(title = "VM名称", required = false)
    val vmName: String? = null,

    @get:Schema(title = "渠道代码", required = false)
    val channelCode: String? = null,

    @get:Schema(title = "密钥", required = false)
    val secretKey: String? = null,

    @get:Schema(title = "系统版本", required = false)
    val systemVersion: String? = null,

    @get:Schema(title = "Xcode版本", required = false)
    val xcodeVersion: String? = null,

    @get:Schema(title = "错误信息", required = false)
    val message: String? = null
)