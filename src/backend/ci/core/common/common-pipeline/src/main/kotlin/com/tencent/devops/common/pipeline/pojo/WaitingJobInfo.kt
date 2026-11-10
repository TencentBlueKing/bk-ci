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

package com.tencent.devops.common.pipeline.pojo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 等待中的Job详情，用于执行态卡片的「等待中的 Job」列表与定位跳转。
 *
 * 位置编码与组件路径的口径与 [EndPosition] 完全一致（如 `2-1` / `构建编译/低端构建`），
 * 均由 Model 位置索引统一生成，保证运行态与终态在页面上的定位表达不会打架。
 */
@Schema(title = "等待中的Job详情")
data class WaitingJobInfo(
    @get:Schema(title = "等待类型", required = true)
    val waitType: JobWaitType,
    @get:Schema(title = "等待类型描述(国际化)", required = false)
    var waitTypeDesc: String? = null,
    @get:Schema(title = "位置编码(如2-1表示Stage2的Job1)", required = true)
    val position: String,
    @get:Schema(title = "组件路径(如: 构建编译/低端构建)", required = true)
    val componentPath: String,
    @get:Schema(title = "Job当前状态(BuildStatus.name)", required = true)
    val status: String,
    @get:Schema(title = "Job当前状态描述(国际化)", required = false)
    var statusDesc: String? = null,
    @get:Schema(title = "阶段ID(锚点定位)", required = true)
    val stageId: String,
    @get:Schema(title = "容器ID(锚点定位)", required = true)
    val containerId: String,
    @get:Schema(title = "容器HashId(日志跳转)", required = false)
    val containerHashId: String? = null,
    @get:Schema(title = "是否构建矩阵", required = false)
    val matrixFlag: Boolean? = null,
    @get:Schema(title = "互斥组名称(互斥排队时填充)", required = false)
    val mutexGroup: String? = null,
    @get:Schema(title = "等待原因(如构建机复用互斥等待被依赖节点调度)", required = false)
    var waitReason: String? = null,
    @get:Schema(title = "已等待时长(毫秒)", required = false)
    val waitingTime: Long? = null,
    @get:Schema(title = "依赖Job导航信息(依赖等待时填充)", required = false)
    val dependOnJobs: List<DependOnJobInfo>? = null
)

/**
 * Job 等待类型。
 *
 * 三者在引擎中的表现形态完全不同，不能仅凭 Job 状态区分：
 * - [MUTEX] 与 [DEPENDENT] 的容器状态分别是 `QUEUE` 和 `DEPENDENT_WAITING`
 * - [RESOURCE] 的容器状态是 `PREPARE_ENV`，与「正常准备环境」共用同一状态，
 *   有第三方资源排队时间戳时立即确认；否则仅在准备环境持续超过宽限期后才上报，
 *   避免把正常拉起构建机的几秒误报成资源排队
 */
enum class JobWaitType(val displayName: String) {
    // 互斥组被占用，等待抢锁
    MUTEX("jobWaitMutex"),
    // 等待构建机资源（第三方构建机资源排队）
    RESOURCE("jobWaitResource"),
    // 依赖的Job尚未完成
    DEPENDENT("jobWaitDependent")
}
