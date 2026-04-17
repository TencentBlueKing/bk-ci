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

package com.tencent.devops.ai.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "MCP服务器配置-创建请求")
data class AiMcpServerCreate(
    @get:Schema(title = "显示名称", required = true, example = "iWiki DevOps")
    val serverName: String,
    @get:Schema(title = "MCP服务器URL", required = true, example = "https://prod.mcp.it.woa.com/app_iwiki_mcp/mcp3")
    val serverUrl: String,
    @get:Schema(title = "传输协议：SSE / STREAMABLE_HTTP", required = false, example = "SSE")
    val transportType: String = "SSE",
    @get:Schema(
        title = "请求头JSON",
        required = false,
        example = """{"Authorization":"Bearer tai_pat_xxx"}"""
    )
    val headers: String? = null,
    @get:Schema(
        title = "绑定智能体",
        required = false,
        example = "knowledge_agent",
        description = "supervisor / knowledge_agent / auth_agent 等"
    )
    val bindAgent: String = "knowledge_agent",
    @get:Schema(title = "是否启用", required = false)
    val enabled: Boolean = true
)
