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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-列表-状态信息")
data class PipelineStatus(
    @get:Schema(title = "流水线任务数量", required = true)
    val taskCount: Int,
    @get:Schema(title = "构建次数", required = true)
    val buildCount: Long,
    @get:Schema(title = "运行锁定", required = false)
    val lock: Boolean,
    @get:Schema(title = "是否可手工启动", required = true)
    val canManualStartup: Boolean,
    @get:Schema(title = "最后构建时间", required = false)
    val latestBuildStartTime: Long?,
    @get:Schema(title = "最后构建结束时间", required = false)
    val latestBuildEndTime: Long?,
    @get:Schema(title = "最后构建状态", required = false)
    val latestBuildStatus: BuildStatus?,
    @get:Schema(title = "最后构建版本号", required = false)
    val latestBuildNum: Int?,
    @get:Schema(title = "最后构建任务名称", required = false)
    @Deprecated("无用.不再提供任何信息")
    val latestBuildTaskName: String? = null,
    @get:Schema(title = "最后任务预计执行时间（毫秒）", required = false)
    val latestBuildEstimatedExecutionSeconds: Long?,
    @get:Schema(title = "最后构建实例ID", required = false)
    val latestBuildId: String?,
    @get:Schema(title = "服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @get:Schema(title = "当前运行的构建的个数", required = true)
    val runningBuildCount: Int,
    @get:Schema(title = "是否被收藏", required = true)
    val hasCollect: Boolean,
    @get:Schema(title = "最后一次构建所有的任务个数", required = false)
    var lastBuildTotalCount: Int? = null,
    @get:Schema(title = "最后一次构建已完成的任务个数", required = false)
    var lastBuildFinishCount: Int? = null,
    @get:Schema(title = "触发方式", required = false)
    var trigger: String? = null,
    @get:Schema(title = "最后一次构建各阶段状态", required = true)
    var latestBuildStageStatus: List<BuildStageStatus>? = null
)
