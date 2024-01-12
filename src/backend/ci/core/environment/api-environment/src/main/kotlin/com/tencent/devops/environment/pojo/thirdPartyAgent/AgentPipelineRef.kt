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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "第三方构建机流水线引用信息")
data class AgentPipelineRef(
    @Schema(description = "Node ID", required = true)
    val nodeId: Long? = null,
    @Schema(description = "Node Hash ID", required = true)
    val nodeHashId: String? = null,
    @Schema(description = "Agent ID", required = true)
    val agentId: Long? = null,
    @Schema(description = "Agent Hash ID", required = true)
    val agentHashId: String? = null,
    @Schema(description = "项目ID", required = true)
    val projectId: String,
    @Schema(description = "流水线ID", required = true)
    val pipelineId: String,
    @Schema(description = "流水线名称", required = true)
    val pipelineName: String,
    @Schema(description = "Vm Seq ID", required = true)
    val vmSeqId: String?,
    @Schema(description = "Job ID", required = true)
    val jobId: String?,
    @Schema(description = "Job Name", required = true)
    val jobName: String,
    @Schema(description = "上次构建时间", required = false)
    val lastBuildTime: String? = ""
)
