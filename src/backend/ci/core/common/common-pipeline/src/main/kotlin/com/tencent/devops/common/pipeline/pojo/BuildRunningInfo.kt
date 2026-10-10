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

import com.tencent.devops.common.pipeline.enums.BuildRunningCategory
import com.tencent.devops.common.pipeline.enums.BuildRunningType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 构建运行态详情——统一描述排队中/执行中两类未结束场景。
 *
 * **与 [BuildEndInfo] 的关键差异：本对象不落库，完全在读取时计算。**
 * 排队位置、前方排队列表、已等待时长、待人工处理项都随时间实时变化，
 * 落库既无意义（读到的必然是过期快照）又会带来大量写放大——排队中的构建每秒都在变。
 * 因此只在 `getBuildRecordByExecuteCount` 读取且构建未结束时现算，构建一旦结束就不再返回。
 *
 * 时长类字段（[waitingTime]/[runningTime]）以服务端计算时刻为准，
 * 前端可结合 `ModelRecord.currentTimestamp` 做秒级自增，避免频繁轮询。
 */
@Schema(title = "构建运行态详情")
data class BuildRunningInfo(
    @get:Schema(title = "运行态子类型", required = true)
    val runningType: BuildRunningType,
    @get:Schema(title = "运行态大类(恒等于runningType所属大类，供前端归类渲染)", required = false)
    var runningCategory: BuildRunningCategory? = null,
    @get:Schema(title = "运行态子类型描述(国际化)", required = false)
    var runningTypeDesc: String? = null,
    @get:Schema(title = "当前阶段描述(执行态: 运行中的Stage名; 无Job在跑时为「Job正在排队」等提示)", required = false)
    var currentPhase: String? = null,
    @get:Schema(title = "入队时间(毫秒)", required = false)
    val queueTime: Long? = null,
    @get:Schema(title = "开始执行时间(毫秒)", required = false)
    val startTime: Long? = null,
    @get:Schema(title = "已等待时长(毫秒，排队态)", required = false)
    val waitingTime: Long? = null,
    @get:Schema(title = "已运行时长(毫秒，执行态)", required = false)
    val runningTime: Long? = null,
    @get:Schema(title = "触发人", required = false)
    val triggerUser: String? = null,
    @get:Schema(title = "触发方式描述", required = false)
    val triggerDesc: String? = null,
    @get:Schema(title = "排队详情(排队态填充)", required = false)
    val queueDetail: BuildQueueDetail? = null,
    @get:Schema(title = "等待中的Job列表(互斥排队/资源排队/依赖等待)", required = false)
    var waitingJobs: List<WaitingJobInfo>? = null,
    @get:Schema(title = "等待中的Job总数", required = false)
    var waitingJobCount: Int = 0,
    @get:Schema(title = "待人工处理项列表", required = false)
    var pendingItems: List<PendingManualItem>? = null,
    @get:Schema(title = "待人工处理项总数", required = false)
    var pendingItemCount: Int = 0
) {
    /**
     * 统一设置等待中的Job列表，保证列表与计数不会出现不一致。
     * 空列表时置空，避免前端渲染出空的区块。
     */
    fun withWaitingJobs(jobs: List<WaitingJobInfo>, totalCount: Int = jobs.size): BuildRunningInfo {
        waitingJobs = jobs.takeIf { it.isNotEmpty() }
        waitingJobCount = totalCount
        return this
    }

    /**
     * 统一设置待人工处理项列表，保证列表与计数不会出现不一致。
     */
    fun withPendingItems(items: List<PendingManualItem>, totalCount: Int = items.size): BuildRunningInfo {
        pendingItems = items.takeIf { it.isNotEmpty() }
        pendingItemCount = totalCount
        return this
    }
}

/**
 * 排队详情：排队位置与阻塞来源。
 *
 * [occupyingBuilds] 与 [aheadBuilds] 回答用户最关心的「我在等谁」：
 * 前者是正占用并发额度的运行中构建，后者是排在自己前面的构建。
 */
@Schema(title = "构建排队详情")
data class BuildQueueDetail(
    @get:Schema(title = "排队位置(从1开始，取不到时为0)", required = true)
    val queuePosition: Int,
    @get:Schema(title = "并发组名称(未配置并发组时为空)", required = false)
    val concurrencyGroup: String? = null,
    @get:Schema(title = "当前生效的最大可并发数(与流水线执行设置一致)", required = false)
    var maxConcurrency: Int? = null,
    @get:Schema(title = "占用中的运行构建列表", required = false)
    var occupyingBuilds: List<RelatedBuildBrief>? = null,
    @get:Schema(title = "占用中的运行构建总数", required = false)
    var occupyingCount: Int = 0,
    @get:Schema(title = "前方排队的构建列表", required = false)
    var aheadBuilds: List<RelatedBuildBrief>? = null,
    @get:Schema(title = "前方排队的构建总数", required = false)
    var aheadCount: Int = 0
)

/**
 * 关联构建摘要，用于「占用中」「前方排队」列表项展示与跳转。
 * 并发组可跨流水线，因此必须携带 projectId/pipelineId 供前端拼跳转链接。
 */
@Schema(title = "关联构建摘要")
data class RelatedBuildBrief(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线名称", required = false)
    val pipelineName: String? = null,
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "构建号", required = false)
    val buildNum: Int? = null,
    @get:Schema(title = "触发人", required = false)
    val triggerUser: String? = null,
    @get:Schema(title = "触发方式描述", required = false)
    val triggerDesc: String? = null,
    @get:Schema(title = "构建信息(如webhook提交信息)", required = false)
    val buildMsg: String? = null,
    @get:Schema(title = "构建状态(BuildStatus.name)", required = true)
    val status: String,
    @get:Schema(title = "已运行/已等待时长(毫秒)", required = false)
    val costTime: Long? = null
)
