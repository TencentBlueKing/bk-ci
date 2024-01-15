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

package com.tencent.devops.process.pojo.app.pipeline

import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "app流水线信息")
data class AppPipeline(
    @Schema(name = "项目id", required = false)
    val projectId: String,
    @Schema(name = "项目名称", required = false)
    var projectName: String,
    @Schema(name = "流水线id", required = false)
    val pipelineId: String,
    @Schema(name = "流水线名称", required = false)
    var pipelineName: String,
    @Schema(name = "流水线描述", required = false)
    var pipelineDesc: String,
    @Schema(name = "最新构建状态", required = false)
    val latestBuildStatus: BuildStatus?,
    @Schema(name = "最近一次构建序号", required = false)
    val latestBuildNum: Int?,
    @Schema(name = "最新构建号id", required = false)
    val latestBuildId: String?,
    @Schema(name = "最近构建启动时间", required = false)
    val latestBuildStartTime: Long?,
    @Schema(name = "最近构建结束时间", required = false)
    val latestBuildEndTime: Long?,
    @Schema(name = "最近构建用户", required = false)
    var latestBuildUser: String,
    @Schema(name = "流水线版本", required = false)
    val pipelineVersion: Int,
    @Schema(name = "是否可手工启动", required = true)
    val canManualStartup: Boolean,
    var hasCollect: Boolean = false,
    @Schema(name = "部署时间", required = true)
    val deploymentTime: Long = 0L,
    @Schema(name = "流水线创建时间", required = true)
    val createTime: Long = 0L,
    @Schema(name = "项目图标链接", required = false)
    val logoUrl: String = ""
)
