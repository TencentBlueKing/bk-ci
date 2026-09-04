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

package com.tencent.devops.process.engine.interceptor

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_MAX_PARALLEL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_QUEUE_FULL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_SUMMARY_NOT_FOUND
import com.tencent.devops.process.engine.control.lock.ConcurrencyGroupLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.Response
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.service.PipelineRedisService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import kotlin.math.max
import kotlin.math.min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 队列拦截, 在外面业务逻辑中需要保证Summary数据的并发控制，否则可能会出现不准确的情况
 * @version 1.0
 */
@Component
@Suppress("ALL")
class QueueInterceptor @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val pipelineRedisService: PipelineRedisService,
    private val pipelineUrlBean: PipelineUrlBean
) : PipelineInterceptor {

    companion object {
        private val logger = LoggerFactory.getLogger(QueueInterceptor::class.java)

        /**
         * 单次满员判定最多淘汰的排队构建数量。
         * 存量队列可能远超 maxQueueSize，分批淘汰避免一次产生过多取消事件、长时间占用并发组锁。
         */
        private const val MAX_OUT_QUEUE_SIZE_PER_CHECK = 50

        /**
         * 排队中的构建状态：QUEUE 为等待被领取，QUEUE_CACHE 为已被领取、等待启动的中间态，
         * 两者都还未开始执行，都应计入排队数量。与 PipelineBuildDao.getOneConcurrencyQueueBuild 的查询口径保持一致。
         */
        private val QUEUE_STATUS_LIST = listOf(BuildStatus.QUEUE, BuildStatus.QUEUE_CACHE)
    }

    override fun execute(task: InterceptData): Response<BuildStatus> {
        if (task.retryOnRunningBuild) {
            // perf：流水线运行中、重试失败的步骤时提示队列满问题跟进和优化 #11807
            // 运行中重试不进行构建流程判断
            return Response(BuildStatus.RUNNING)
        }
        val projectId = task.pipelineInfo.projectId
        val pipelineId = task.pipelineInfo.pipelineId
        val runLockType = task.runLockType
        val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(projectId, pipelineId)
        return when {
            buildSummaryRecord == null ->
                // Summary为空是不正常的，抛错
                Response(
                    status = ERROR_PIPELINE_SUMMARY_NOT_FOUND.toInt(),
                    message = MessageUtil.getMessageByLocale(
                        messageCode = ERROR_PIPELINE_SUMMARY_NOT_FOUND,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )

            runLockType == PipelineRunLockType.SINGLE || runLockType == PipelineRunLockType.SINGLE_LOCK ->
                checkRunLockWithSingleType(
                    task = task,
                    latestBuildId = buildSummaryRecord.latestBuildId,
                    latestStartUser = buildSummaryRecord.latestStartUser,
                    runningCount = buildSummaryRecord.runningCount,
                    queueCount = buildSummaryRecord.queueCount
                )

            runLockType == PipelineRunLockType.GROUP_LOCK ->
                checkRunLockWithGroupType(
                    task = task,
                    latestBuildId = buildSummaryRecord.latestBuildId,
                    latestStartUser = buildSummaryRecord.latestStartUser,
                    runningCount = buildSummaryRecord.runningCount
                )

            (buildSummaryRecord.queueCount + buildSummaryRecord.runningCount) >= max(
                PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX,
                task.maxConRunningQueueSize
            ) ->
                Response(
                    status = ERROR_PIPELINE_QUEUE_FULL.toInt(),
                    message = MessageUtil.getMessageByLocale(
                        messageCode = BK_MAX_PARALLEL,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + " ${max(PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX, task.maxConRunningQueueSize)}"
                )

            else -> Response(data = BuildStatus.RUNNING)
        }
    }

    private fun checkRunLockWithSingleType(
        task: InterceptData,
        latestBuildId: String?,
        latestStartUser: String?,
        runningCount: Int,
        queueCount: Int,
        groupName: String? = null
    ): Response<BuildStatus> {
        val projectId = task.pipelineInfo.projectId
        val pipelineId = task.pipelineInfo.pipelineId
        return when {
            // 如果最后一次构建被标记为refresh,则即便是串行也放行。因refresh的buildId都会被取消掉
            latestBuildId == null || pipelineRedisService.getBuildRestartValue(latestBuildId) != null ->
                Response(data = BuildStatus.RUNNING)
            // 设置了最大排队数量限制为0，但此时没有构建正在执行
            task.maxQueueSize == 0 && runningCount == 0 && queueCount == 0 ->
                Response(data = BuildStatus.RUNNING)

            task.maxQueueSize == 0 && (runningCount > 0 || queueCount > 0) ->
                Response(
                    status = ERROR_PIPELINE_QUEUE_FULL.toInt(),
                    message = MessageUtil.getMessageByLocale(
                        messageCode = ERROR_PIPELINE_QUEUE_FULL,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    )
                )

            queueCount >= task.maxQueueSize -> {
                // 满员时挤掉最早排队的构建并放行当前构建，maxQueueSize是排队队列的容量而非新构建的准入门槛。
                // 淘汰数量按超出量计算：队列未超限时与历史行为一致(淘汰1个)；
                // 若队列已经超限(如并发触发时多个构建同时通过了满员判定)，则多淘汰几个使排队数量回落，避免只进不出。
                val outQueueSize = min(queueCount - task.maxQueueSize + 1, MAX_OUT_QUEUE_SIZE_PER_CHECK)
                if (groupName == null) {
                    outQueueCancelBySingle(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        latestStartUser = latestStartUser,
                        task = task,
                        outQueueSize = outQueueSize
                    )
                } else {
                    outQueueCancelByGroup(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        groupName = groupName,
                        latestStartUser = latestStartUser,
                        task = task,
                        outQueueSize = outQueueSize
                    )
                }
                Response(data = BuildStatus.RUNNING)
            }
            // 满足条件
            else ->
                Response(data = BuildStatus.RUNNING)
        }
    }

    private fun outQueueCancelBySingle(
        projectId: String,
        pipelineId: String,
        latestStartUser: String?,
        task: InterceptData,
        outQueueSize: Int
    ) {
        if (!task.cancelAllowed) {
            return
        }
        // 排队数量超过最大限制,排队数量已满，将该流水线最靠前的排队记录，置为"取消构建"
        val outQueueBuilds = mutableListOf<BuildInfo>()
        while (outQueueBuilds.size < outQueueSize) {
            val buildInfo = pipelineRuntimeExtService.popNextQueueBuildInfo(
                projectId = projectId,
                pipelineId = pipelineId,
                buildStatus = BuildStatus.UNEXEC
            ) ?: break // 队列已空，无需继续淘汰
            // 出队状态更新失败时会重复取到同一个构建，此时终止淘汰，避免重复发送取消事件
            if (outQueueBuilds.any { it.buildId == buildInfo.buildId }) {
                break
            }
            outQueueBuilds.add(buildInfo)
        }
        if (outQueueBuilds.isEmpty()) {
            return
        }
        logger.info("[$pipelineId]|${task.buildId}|QUEUE_OUT_SIZE|single|out=${outQueueBuilds.size}")
        outQueueBuilds.forEach { buildInfo ->
            buildLogPrinter.addRedLine(
                buildId = buildInfo.buildId,
                message = "[$pipelineId] queue outSize,cancel first Queue build",
                tag = "QueueInterceptor",
                containerHashId = "",
                executeCount = 1,
                jobId = null,
                stepId = "QueueInterceptor"
            )
            pipelineEventDispatcher.dispatch(
                PipelineBuildCancelEvent(
                    source = javaClass.simpleName,
                    projectId = buildInfo.projectId,
                    pipelineId = pipelineId,
                    userId = latestStartUser ?: task.pipelineInfo.creator,
                    buildId = buildInfo.buildId,
                    status = BuildStatus.CANCELED,
                    executeCount = buildInfo.executeCount
                )
            )
        }
    }

    private fun outQueueCancelByGroup(
        projectId: String,
        pipelineId: String,
        groupName: String,
        latestStartUser: String?,
        task: InterceptData,
        outQueueSize: Int
    ) {
        if (!task.cancelAllowed) {
            return
        }
        // 因为排队队列是流水线级别，所以是取消当前流水线下同一并发组最早排队的构建，不一定是项目级别下同一并发组最早的构建。
        // 锁内只做出队，日志与取消事件放到锁外处理，避免批量淘汰时长时间占用并发组锁阻塞构建启动。
        val outQueueBuilds = mutableListOf<BuildInfo>()
        ConcurrencyGroupLock(redisOperation, projectId, groupName).use { pipelineLock ->
            pipelineLock.lock()
            while (outQueueBuilds.size < outQueueSize) {
                val buildInfo = pipelineRuntimeExtService.popNextConcurrencyGroupQueueCanPend2Start(
                    projectId = projectId,
                    concurrencyGroup = groupName,
                    pipelineId = pipelineId,
                    buildStatus = BuildStatus.UNEXEC
                ) ?: break // 队列已空，无需继续淘汰
                // 出队状态更新失败时会重复取到同一个构建，此时终止淘汰，避免重复发送取消事件
                if (outQueueBuilds.any { it.buildId == buildInfo.buildId }) {
                    break
                }
                outQueueBuilds.add(buildInfo)
            }
        }
        if (outQueueBuilds.isEmpty()) {
            return
        }
        logger.info("[$pipelineId]|${task.buildId}|QUEUE_OUT_SIZE|$groupName|out=${outQueueBuilds.size}")
        val detailUrl = pipelineUrlBean.genBuildDetailUrl(
            projectCode = projectId,
            pipelineId = task.pipelineInfo.pipelineId,
            buildId = task.buildId,
            position = null,
            stageId = null,
            needShortUrl = false
        )
        outQueueBuilds.forEach { buildInfo ->
            buildLogPrinter.addRedLine(
                buildId = buildInfo.buildId,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_BUILD_QUEUE_WAIT_FOR_CONCURRENCY,
                    params = arrayOf(
                        groupName,
                        "<a target='_blank' href='$detailUrl'>${task.buildId}</a>"
                    )
                ),
                tag = "QueueInterceptor",
                containerHashId = "",
                executeCount = 1,
                jobId = null,
                stepId = "QueueInterceptor"
            )
            pipelineEventDispatcher.dispatch(
                PipelineBuildCancelEvent(
                    source = javaClass.simpleName,
                    projectId = buildInfo.projectId,
                    pipelineId = pipelineId,
                    userId = latestStartUser ?: task.pipelineInfo.creator,
                    buildId = buildInfo.buildId,
                    status = BuildStatus.CANCELED,
                    executeCount = buildInfo.executeCount
                )
            )
        }
    }

    private fun checkRunLockWithGroupType(
        task: InterceptData,
        latestBuildId: String?,
        latestStartUser: String?,
        runningCount: Int
    ): Response<BuildStatus> {
        val projectId = task.pipelineInfo.projectId
        val concurrencyGroup = task.concurrencyGroup ?: task.pipelineInfo.pipelineId
        return when {
            concurrencyGroup.isNotBlank() && task.concurrencyCancelInProgress && task.cancelAllowed -> {
                val detailUrl = pipelineUrlBean.genBuildDetailUrl(
                    projectCode = projectId,
                    pipelineId = task.pipelineInfo.pipelineId,
                    buildId = task.buildId,
                    position = null,
                    stageId = null,
                    needShortUrl = false
                )
                // cancel-in-progress: true时， 若有相同 group 的流水线正在执行，则取消正在执行的流水线，新来的触发开始执行
                // status 取所有没有完成的状态
                val status = BuildStatus.values().filterNot { it.isFinish() }
                val builds = pipelineRuntimeService.getBuildInfoListByConcurrencyGroup(
                    projectId = projectId,
                    concurrencyGroup = concurrencyGroup,
                    status = status
                ).toMutableList()
                // #8143 兼容旧流水线版本 TODO 待模板设置补上漏洞，后期下掉 # 8143
                if (concurrencyGroup == task.pipelineInfo.pipelineId) {
                    builds.addAll(
                        0,
                        pipelineRuntimeService.getBuildInfoListByConcurrencyGroupNull(
                            projectId = projectId,
                            pipelineId = task.pipelineInfo.pipelineId,
                            status = status
                        )
                    )
                }
                builds.forEach { (pipelineId, buildId) ->
                    pipelineRuntimeService.concurrencyCancelBuildPipeline(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        userId = latestStartUser ?: task.pipelineInfo.creator,
                        groupName = concurrencyGroup,
                        detailUrl = detailUrl
                    )
                }
                Response(data = BuildStatus.RUNNING)
            }

            concurrencyGroup.isNotBlank() && !task.concurrencyCancelInProgress -> {
                // cancel-in-progress: false时，保持原有single逻辑
                checkRunLockWithSingleType(
                    task = task,
                    latestBuildId = latestBuildId,
                    latestStartUser = latestStartUser,
                    runningCount = countGroupBuild(
                        task = task,
                        concurrencyGroup = concurrencyGroup,
                        status = listOf(BuildStatus.RUNNING)
                    ),
                    // #7681 在history表中取出当前流水线下相同并发组排队的数量。
                    // #13499 排队数量必须同时统计 QUEUE 与 QUEUE_CACHE，否则头部构建在"领取-回退"
                    // 循环中处于 QUEUE_CACHE 时会被漏统计，导致满员判定偏小、淘汰时机被推迟。
                    queueCount = countGroupBuild(
                        task = task,
                        concurrencyGroup = concurrencyGroup,
                        status = QUEUE_STATUS_LIST
                    ),
                    groupName = concurrencyGroup
                )
            }
            // 满足条件
            else -> Response(data = BuildStatus.RUNNING)
        }
    }

    /**
     * 统计当前流水线下处于[status]的、属于并发组[concurrencyGroup]的构建数量。
     *
     * 排队队列是流水线级别的，所以项目级并发组的查询结果需要再按当前流水线过滤。
     *
     * 注意：这里不能像其他分支那样叠加 #8143 的空并发组兼容统计。
     * 满员淘汰依赖 getOneConcurrencyQueueBuild，其条件为 CONCURRENCY_GROUP = 并发组，取不到并发组为空的旧构建。
     * 若把这类构建计入排队数量，会出现"判定为满员但一个都淘汰不掉"从而只进不出，
     * 因此统计口径必须与淘汰口径保持一致。
     */
    private fun countGroupBuild(
        task: InterceptData,
        concurrencyGroup: String,
        status: List<BuildStatus>
    ): Int {
        return pipelineRuntimeService.getBuildInfoListByConcurrencyGroup(
            projectId = task.pipelineInfo.projectId,
            concurrencyGroup = concurrencyGroup,
            status = status
        ).count { it.first == task.pipelineInfo.pipelineId }
    }
}
