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

package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

/**
 * #8609 跨项目子流水线重试联动父流水线单步重试请求体
 *
 * 当同步模式触发的子流水线在不同项目/集群被重试时，子集群通过服务间网关路由转发至父项目集群，
 * 父集群接口仅将其转为本地异步 MQ 事件后立即返回，避免跨集群同步阻塞。
 */
@Schema(title = "子流水线重试联动父流水线请求")
data class SubPipelineRetrySyncRequest(
    @get:Schema(title = "父流水线ID", required = true)
    val parentPipelineId: String,
    @get:Schema(title = "父构建ID", required = true)
    val parentBuildId: String,
    @get:Schema(title = "父步骤TaskId", required = true)
    val parentTaskId: String,
    @get:Schema(title = "子项目ID", required = true)
    val subProjectId: String,
    @get:Schema(title = "子流水线ID", required = true)
    val subPipelineId: String,
    @get:Schema(title = "子流水线名称", required = false)
    val subPipelineName: String = "",
    @get:Schema(title = "子构建ID", required = true)
    val subBuildId: String,
    @get:Schema(title = "子构建号", required = false)
    val subBuildNum: Int? = null,
    @get:Schema(title = "子构建本次重试执行次数", required = true)
    val subExecuteCount: Int,
    @get:Schema(title = "触发本次重试的操作人", required = false)
    val operator: String? = null,
    @get:Schema(title = "多级回溯已访问的构建ID链路（用于防环和深度限制）", required = false)
    val visitedBuildIds: List<String> = emptyList()
)
