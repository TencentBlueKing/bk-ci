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

package com.tencent.devops.dispatch.devcloud.service.context

import com.tencent.devops.common.environment.agent.pojo.devcloud.Pool
import com.tencent.devops.common.pipeline.type.DispatchType

class DcStartupHandlerContext(
    val vmSeqId: String,
    val gateway: String,
    val dispatchMessage: String,
    val atoms: Map<String, String> = mapOf(),
    val dispatchType: DispatchType?,
    val customBuildEnv: Map<String, String>? = null,
    val containerHashId: String?,
    val agentId: String,
    val secretKey: String,
    val persistence: Boolean = false,
    var cpu: Int = 16,
    var memory: String = "32768M",
    var disk: String = "100G",
    var buildLogKey: String = "",
    var containerPool: Pool? = null, // 当前构建容器配置信息
    var poolNo: Int = 0, // 容器池序号
    var containerName: String? = null, // 复用的容器池容器
    var containerChanged: Boolean = true, // 复用的容器池容器配置是否已变更，默认true
    var persistenceAgentId: String = "",
    override val executeCount: Int?,
    override val userId: String,
    override val projectId: String,
    override val pipelineId: String,
    override val buildId: String
) : HandlerContext(
    projectId, pipelineId, buildId, userId, executeCount
)
