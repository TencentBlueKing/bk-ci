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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import kotlin.math.min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 并发组排队数量收敛（对账）服务。
 *
 * 背景（#13499）：启动请求路径上的满员判定与排队记录入库不是原子的
 * （`PipelineBuildService.startPipeline` 先跑拦截器读排队数量，之后才由 `startBuild` 插入记录，
 * 两步之间没有流水线级互斥锁），突发触发时大量请求会读到同一个偏小的排队数量并全部放行，
 * 使排队数量冲高到远超 maxQueueSize。
 *
 * 与其在请求路径上加阻塞锁（会让突发流量长时间排队甚至超时），这里改为在引擎侧已经持有
 * 并发组锁的对账时机做强制收敛：此时排队记录都已入库，读到的是真实数量，
 * 因此无论并发多大都能把排队数量裁剪回 maxQueueSize。
 */
@Service
class PipelineConcurrencyQueueTrimService @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineConcurrencyQueueTrimService::class.java)
        private const val TAG = "QueueTrim"

        /**
         * 单轮最多淘汰的排队构建数量。
         * 存量队列可能远超 maxQueueSize，分批淘汰避免一次产生过多取消事件、长时间占用并发组锁。
         */
        private const val MAX_TRIM_SIZE_PER_ROUND = 50
    }

    /**
     * 把并发组[concurrencyGroup]内流水线[pipelineId]的排队数量收敛回[maxQueueSize]。
     *
     * **调用方必须已持有该并发组的 ConcurrencyGroupLock。**
     * @param projectId 项目 ID
     * @param pipelineId 流水线 ID
     * @param concurrencyGroup 并发组
     * @param maxQueueSize 最大排队数量
     * @param protectBuildId 当前正在启动的构建，绝不淘汰。并发组内队头构建既是"排队最久、最该被淘汰"的，
     *                       也是"下一个要执行"的，不排除它会把即将启动的构建取消掉。
     * @param userId 操作人
     * @return 实际淘汰的构建数量
     */
    fun trimGroupQueue(
        projectId: String,
        pipelineId: String,
        concurrencyGroup: String,
        maxQueueSize: Int,
        protectBuildId: String,
        userId: String
    ): Int {
        // maxQueueSize <= 0 表示不允许排队，该语义由启动拦截器把关。
        // 对账逻辑不介入，避免把整个队列清空。
        if (maxQueueSize <= 0) {
            return 0
        }
        // 统计口径与启动拦截器保持一致：QUEUE 等待被领取，QUEUE_CACHE 已被领取待启动，都还没开始执行
        val queueCount = pipelineRuntimeService.getBuildInfoListByConcurrencyGroup(
            projectId = projectId,
            concurrencyGroup = concurrencyGroup,
            status = listOf(BuildStatus.QUEUE, BuildStatus.QUEUE_CACHE)
        ).count { it.first == pipelineId }
        if (queueCount <= maxQueueSize) {
            return 0
        }

        val trimSize = min(queueCount - maxQueueSize, MAX_TRIM_SIZE_PER_ROUND)
        // 只淘汰 QUEUE 状态：QUEUE_CACHE 已被领取、正在走启动流程，取消它会与启动逻辑相互打架。
        // 多取一个是为了在 protectBuildId 恰好也处于 QUEUE 时仍能凑满 trimSize。
        val outQueueBuilds = pipelineRuntimeExtService.listConcurrencyQueueBuilds(
            projectId = projectId,
            concurrencyGroup = concurrencyGroup,
            pipelineId = pipelineId,
            statusSet = listOf(BuildStatus.QUEUE),
            limit = trimSize + 1
        ).filter { it.buildId != protectBuildId }.take(trimSize)

        var trimmed = 0
        outQueueBuilds.forEach { buildInfo ->
            // CAS 出队：状态已被其他流程改变的构建直接跳过，避免误取消
            val outQueue = pipelineRuntimeExtService.changeBuildStatus(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildInfo.buildId,
                oldBuildStatus = BuildStatus.QUEUE,
                newBuildStatus = BuildStatus.UNEXEC
            )
            if (!outQueue) {
                return@forEach
            }
            trimmed++
            buildLogPrinter.addRedLine(
                buildId = buildInfo.buildId,
                message = "[$pipelineId] queue size exceeded limit($maxQueueSize), cancel the earliest queued build",
                tag = TAG,
                containerHashId = "",
                executeCount = buildInfo.executeCount,
                jobId = null,
                stepId = TAG
            )
            pipelineEventDispatcher.dispatch(
                PipelineBuildCancelEvent(
                    source = javaClass.simpleName,
                    projectId = buildInfo.projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildInfo.buildId,
                    status = BuildStatus.CANCELED,
                    executeCount = buildInfo.executeCount
                )
            )
        }
        if (trimmed > 0) {
            logger.info(
                "ENGINE|$pipelineId|QUEUE_TRIM|$concurrencyGroup|queue=$queueCount|" +
                    "max=$maxQueueSize|trimmed=$trimmed"
            )
        }
        return trimmed
    }
}
