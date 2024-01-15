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

package com.tencent.devops.environment.pojo.thirdPartyAgent

import com.tencent.devops.common.api.enums.AgentStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "第三方接入机")
data class ThirdPartyAgent(
    @Schema(name = "Agent Hash ID", required = true)
    val agentId: String,
    @Schema(name = "项目ID", required = true)
    val projectId: String,
    @Schema(name = "节点ID", required = false)
    val nodeId: String?,
    @Schema(name = "状态")
    val status: AgentStatus,
    @Schema(name = "主机名", required = true)
    val hostname: String,
    @Schema(name = "系统", required = true)
    val os: String,
    @Schema(name = "IP地址", required = true)
    val ip: String,
    @Schema(name = "Secret KEY", required = true)
    val secretKey: String,
    @Schema(name = "创建用户", required = true)
    val createUser: String,
    @Schema(name = "创建时间", required = true)
    val createTime: Long,
    @Schema(name = "并行执行的个数", required = false)
    val parallelTaskCount: Int? = 4,
    @Schema(name = "Docker构建机并行执行的个数", required = false)
    val dockerParallelTaskCount: Int? = 4
)
