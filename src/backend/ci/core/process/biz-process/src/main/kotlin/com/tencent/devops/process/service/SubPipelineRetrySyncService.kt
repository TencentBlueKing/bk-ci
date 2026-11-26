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

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.context.ChannelContext
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.api.service.ServiceSubPipelineResource
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.StageIdLock
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.event.SubPipelineRetrySyncEvent
import com.tencent.devops.process.pojo.pipeline.SubPipelineRetrySyncRequest
import com.tencent.devops.process.service.builds.PipelineBuildRetryService
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_START_SUB_RETRY_SYNC
import com.tencent.devops.process.utils.PIPELINE_START_SUB_RUN_MODE
import com.tencent.devops.process.utils.PIPELINE_SUB_PIPELINE_RETRY_SYNC_CHAIN
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * #8609 子流水线同步模式重试联动父流水线单步重试核心服务
 *
 * 整体链路与关键保护机制：
 * 1. 【非阻塞监听】消费既有的 PipelineBuildQueueBroadCastEvent（actionType == RETRY 且 triggerType == PIPELINE），
 *    仅对同步模式且开启插件开关（BK_CI_SUB_PIPELINE_RETRY_SYNC == "true"）的子构建派发独立事件，存量构建 O(1) 过滤；
 * 2. 【跨集群零阻塞转发】跨项目调用时经 ServiceSubPipelineResource 路由到父项目集群，父集群接口仅入队本地 MQ 立即返回；
 * 3. 【非阻塞分布式锁互斥】统一通过 BuildIdLock(parentBuildId).tryLock() 与父侧手动重试/跳过/取消互斥；
 *    抢锁失败或父构建因并行兄弟步骤短暂处于 QUEUE 时立即释放线程，按指数退避延迟重投 MQ，彻底杜绝线程阻塞与父子死锁；
 * 4. 【精准防错写】锁内严格校验 parentTask.subBuildId == subBuildId && parentTask.subProjectId == subProjectId、
 *    插件开关 enableRetrySync == true、父步骤仍为失败态，完全对齐单步重试状态要求（同时支持父构建已结束与运行中重试）；
 * 5. 【复用既有单步重试与防轮询错对象】写入绑定到 (parentBuildId, parentTaskId, expectedParentExecuteCount, subBuildId)
 *    的一次性凭证后复用 PipelineBuildRetryService.retryWithinLock()；父步骤拉起时原子消费该凭证并直接轮询原 subBuildId；
 * 6. 【多级逐级回溯】父构建本身若也是同步子流水线，其单步重试入队时会再次广播 PipelineBuildQueueBroadCastEvent，
 *    通过 visitedBuildIds 自动向上级联并做环路与最大深度保护。
 */
@Suppress("LongParameterList", "ReturnCount", "ComplexMethod", "NestedBlockDepth")
@Service
class SubPipelineRetrySyncService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineBuildRetryService: PipelineBuildRetryService,
    private val subPipelineRetryAttachService: SubPipelineRetryAttachService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineRetrySyncService::class.java)
        private const val SYNC_RUN_MODE = "syn"
        private const val MAX_SYNC_DEPTH = 20
        private val LOCK_RETRY_BACKOFF_MILLS = intArrayOf(1000, 2000, 4000, 8000, 15000)
    }

    /**
     * 子侧轻量前置过滤：仅对同步模式且显式开启插件级重试同步开关的子构建派发联动事件。
     */
    fun onBuildQueue(event: PipelineBuildQueueBroadCastEvent) {
        if (event.triggerType != StartType.PIPELINE.name || event.actionType != ActionType.RETRY) {
            return
        }
        try {
            val keys = setOf(
                PIPELINE_START_SUB_RUN_MODE,
                PIPELINE_START_SUB_RETRY_SYNC,
                PIPELINE_START_PARENT_PROJECT_ID,
                PIPELINE_START_PARENT_PIPELINE_ID,
                PIPELINE_START_PARENT_BUILD_ID,
                PIPELINE_START_PARENT_BUILD_TASK_ID,
                PIPELINE_SUB_PIPELINE_RETRY_SYNC_CHAIN
            )
            val vars = pipelineRuntimeService.getBuildVariableService(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                keys = keys
            )
            if (vars[PIPELINE_START_SUB_RUN_MODE] != SYNC_RUN_MODE) {
                return
            }
            if (!vars[PIPELINE_START_SUB_RETRY_SYNC].equals("true", ignoreCase = true)) {
                return
            }
            val parentProjectId = vars[PIPELINE_START_PARENT_PROJECT_ID]
            val parentPipelineId = vars[PIPELINE_START_PARENT_PIPELINE_ID]
            val parentBuildId = vars[PIPELINE_START_PARENT_BUILD_ID]
            val parentTaskId = vars[PIPELINE_START_PARENT_BUILD_TASK_ID]
            if (parentProjectId.isNullOrBlank() ||
                parentPipelineId.isNullOrBlank() ||
                parentBuildId.isNullOrBlank() ||
                parentTaskId.isNullOrBlank()
            ) {
                logger.warn(
                    "SUB_PIPELINE_RETRY_SYNC|incomplete parent info|subBuildId=${event.buildId}|vars=$vars"
                )
                return
            }

            val subBuildInfo = pipelineRuntimeService.getBuildInfo(event.projectId, event.buildId) ?: return
            // 与构建主表记录的父构建/父步骤交叉校验，防止脏变量导致回写到错误父构建
            if ((!subBuildInfo.parentBuildId.isNullOrBlank() && subBuildInfo.parentBuildId != parentBuildId) ||
                (!subBuildInfo.parentTaskId.isNullOrBlank() && subBuildInfo.parentTaskId != parentTaskId)
            ) {
                logger.warn(
                    "SUB_PIPELINE_RETRY_SYNC|parent anchor mismatch|subBuildId=${event.buildId}|" +
                        "varParent=$parentBuildId/$parentTaskId|" +
                        "dbParent=${subBuildInfo.parentBuildId}/${subBuildInfo.parentTaskId}"
                )
                return
            }

            val subPipelineName = pipelineRepositoryService.getPipelineInfo(
                projectId = event.projectId,
                pipelineId = event.pipelineId
            )?.pipelineName ?: event.pipelineId

            val previousVisited = parseVisitedBuildIds(vars[PIPELINE_SUB_PIPELINE_RETRY_SYNC_CHAIN])
            val visitedBuildIds = (previousVisited + event.buildId).distinct()
            if (visitedBuildIds.size > MAX_SYNC_DEPTH || parentBuildId in visitedBuildIds) {
                logger.error(
                    "SUB_PIPELINE_RETRY_SYNC|cycle or max depth reached|subBuildId=${event.buildId}|" +
                        "parentBuildId=$parentBuildId|visited=$visitedBuildIds"
                )
                return
            }

            sampleEventDispatcher.dispatch(
                SubPipelineRetrySyncEvent(
                    parentProjectId = parentProjectId,
                    parentPipelineId = parentPipelineId,
                    parentBuildId = parentBuildId,
                    parentTaskId = parentTaskId,
                    subProjectId = event.projectId,
                    subPipelineId = event.pipelineId,
                    subPipelineName = subPipelineName,
                    subBuildId = event.buildId,
                    subBuildNum = subBuildInfo.buildNum,
                    subExecuteCount = subBuildInfo.executeCount ?: 1,
                    operator = event.userId,
                    visitedBuildIds = visitedBuildIds
                )
            )
        } catch (ignored: Exception) {
            logger.warn("SUB_PIPELINE_RETRY_SYNC|onBuildQueue error|subBuildId=${event.buildId}", ignored)
        }
    }

    /**
     * 消费联动事件：跨项目经服务间网关转发至父项目集群（入队父集群本地 MQ）；同项目直接本地处理。
     */
    fun onRetrySyncEvent(event: SubPipelineRetrySyncEvent) {
        if (!event.fromRemoteForward && event.parentProjectId != event.subProjectId) {
            try {
                client.getGateway(ServiceSubPipelineResource::class).syncParentOnSubRetry(
                    parentProjectId = event.parentProjectId,
                    request = SubPipelineRetrySyncRequest(
                        parentPipelineId = event.parentPipelineId,
                        parentBuildId = event.parentBuildId,
                        parentTaskId = event.parentTaskId,
                        subProjectId = event.subProjectId,
                        subPipelineId = event.subPipelineId,
                        subPipelineName = event.subPipelineName,
                        subBuildId = event.subBuildId,
                        subBuildNum = event.subBuildNum,
                        subExecuteCount = event.subExecuteCount,
                        operator = event.operator,
                        visitedBuildIds = event.visitedBuildIds
                    )
                )
            } catch (ignored: Exception) {
                logger.warn(
                    "SUB_PIPELINE_RETRY_SYNC|forward cross-project failed|" +
                        "parent=${event.parentProjectId}/${event.parentBuildId}|sub=${event.subBuildId}",
                    ignored
                )
            }
            return
        }
        syncParentOnSubRetry(event)
    }

    /**
     * 父集群本地处理入口：由 ServiceSubPipelineResourceImpl 调用，仅异步入队不阻塞跨集群 HTTP 请求。
     */
    fun enqueueLocalSyncEvent(parentProjectId: String, request: SubPipelineRetrySyncRequest) {
        sampleEventDispatcher.dispatch(
            SubPipelineRetrySyncEvent(
                parentProjectId = parentProjectId,
                parentPipelineId = request.parentPipelineId,
                parentBuildId = request.parentBuildId,
                parentTaskId = request.parentTaskId,
                subProjectId = request.subProjectId,
                subPipelineId = request.subPipelineId,
                subPipelineName = request.subPipelineName,
                subBuildId = request.subBuildId,
                subBuildNum = request.subBuildNum,
                subExecuteCount = request.subExecuteCount,
                operator = request.operator,
                visitedBuildIds = request.visitedBuildIds,
                fromRemoteForward = true
            )
        )
    }

    /**
     * 在父项目集群本地非阻塞抢占 BuildIdLock(parentBuildId)，校验单步重试条件并联动触发父步骤单步重试。
     */
    internal fun syncParentOnSubRetry(event: SubPipelineRetrySyncEvent) {
        if (event.visitedBuildIds.size > MAX_SYNC_DEPTH || event.parentBuildId in event.visitedBuildIds) {
            logger.error(
                "SUB_PIPELINE_RETRY_SYNC|cycle or max depth blocked|" +
                    "parent=${event.parentBuildId}|visited=${event.visitedBuildIds}"
            )
            return
        }

        val buildLock = BuildIdLock(redisOperation = redisOperation, buildId = event.parentBuildId)
        if (!buildLock.tryLock()) {
            requeueWithBackoff(event, "build_lock_busy")
            return
        }

        var stageLock: StageIdLock? = null
        var needRequeueReason: String? = null
        try {
            val parentBuildInfo = pipelineRuntimeService.getBuildInfo(
                projectId = event.parentProjectId,
                buildId = event.parentBuildId
            ) ?: run {
                logger.info("SUB_PIPELINE_RETRY_SYNC|skip|parent build not found|${event.parentBuildId}")
                return
            }
            if (parentBuildInfo.pipelineId != event.parentPipelineId) {
                logger.warn(
                    "SUB_PIPELINE_RETRY_SYNC|skip|parent pipelineId mismatch|" +
                        "expected=${event.parentPipelineId},actual=${parentBuildInfo.pipelineId}"
                )
                return
            }

            val parentTask = pipelineTaskService.getByTaskId(
                transactionContext = null,
                projectId = event.parentProjectId,
                buildId = event.parentBuildId,
                taskId = event.parentTaskId
            ) ?: run {
                logger.info(
                    "SUB_PIPELINE_RETRY_SYNC|skip|parent task not found|${event.parentBuildId}|${event.parentTaskId}"
                )
                return
            }

            // 1. 锁内权威校验插件级开关（存量未配置或 false 一律拦截）
            if (!SubPipelineRetryAttachService.isRetrySyncEnabled(parentTask)) {
                logger.info(
                    "SUB_PIPELINE_RETRY_SYNC|skip|plugin retry sync disabled|" +
                        "${event.parentBuildId}|${event.parentTaskId}"
                )
                return
            }

            // 2. 精确子构建锚定校验：防止父侧已手动重跑指向新子构建 B2 后，旧子构建 B1 重试产生脏触发
            if (parentTask.subBuildId != event.subBuildId || parentTask.subProjectId != event.subProjectId) {
                logger.info(
                    "SUB_PIPELINE_RETRY_SYNC|skip|subBuildId anchor mismatch|${event.parentBuildId}|" +
                        "${event.parentTaskId}|taskSub=${parentTask.subProjectId}/${parentTask.subBuildId}|" +
                        "eventSub=${event.subProjectId}/${event.subBuildId}"
                )
                return
            }

            // 3. 父步骤状态校验：仅当父步骤仍为失败态才允许联动重试（排除已跳过 SKIP、已成功 SUCCEED、正在重试 QUEUE/RUNNING、已取消 CANCELED）
            if (!parentTask.status.isFailure()) {
                logger.info(
                    "SUB_PIPELINE_RETRY_SYNC|skip|parent task not failed|${event.parentBuildId}|" +
                        "${event.parentTaskId}|status=${parentTask.status}"
                )
                return
            }

            // 4. 父构建三形态状态校验（完全对齐单步重试状态要求，不强制要求整体必须 FAILED）
            val isRunningRetry: Boolean
            when {
                // 形态 1：父构建正因并行兄弟步骤重试而短暂处于 QUEUE/QUEUE_CACHE，
                // 而本步骤仍为 FAILED 且锚定 subBuildId 吻合 -> 释放锁退避重投，待父构建进入 RUNNING 后走运行中局部重试
                !parentBuildInfo.isFinish() && parentBuildInfo.status.isReadyToRun() -> {
                    needRequeueReason = "parent_build_queueing"
                    return
                }

                // 形态 2：父构建已结束 -> 排除用户主动取消/终止与已成功态，其余失败终态均支持单步重试
                parentBuildInfo.isFinish() -> {
                    if (parentBuildInfo.status.isCancel() || parentBuildInfo.status.isSuccess()) {
                        logger.info(
                            "SUB_PIPELINE_RETRY_SYNC|skip|parent finished with status=${parentBuildInfo.status}|" +
                                "${event.parentBuildId}"
                        )
                        return
                    }
                    isRunningRetry = false
                }

                // 形态 3：父构建正在运行中 -> 对齐 runningBuildRetry 规则（Stage 运行中 + Container 已结束 + 非阻塞抢 StageIdLock）
                else -> {
                    if (parentBuildInfo.isStageSuccess()) {
                        logger.info(
                            "SUB_PIPELINE_RETRY_SYNC|skip|parent stage waiting review|${event.parentBuildId}"
                        )
                        return
                    }
                    val lock = StageIdLock(redisOperation, event.parentBuildId, parentTask.stageId)
                    if (!lock.tryLock()) {
                        needRequeueReason = "stage_lock_busy"
                        return
                    }
                    stageLock = lock

                    val stage = pipelineStageService.getStage(
                        projectId = event.parentProjectId,
                        buildId = event.parentBuildId,
                        stageId = parentTask.stageId
                    )
                    val container = pipelineContainerService.getContainer(
                        projectId = event.parentProjectId,
                        buildId = event.parentBuildId,
                        stageId = parentTask.stageId,
                        containerId = parentTask.containerId
                    )
                    if (stage == null || !stage.status.isRunning() || container == null || !container.status.isFinish()) {
                        logger.info(
                            "SUB_PIPELINE_RETRY_SYNC|skip|running parent not eligible|" +
                                "${event.parentBuildId}|stage=${stage?.status}|container=${container?.status}"
                        )
                        return
                    }
                    isRunningRetry = true
                }
            }

            // 5. 父流水线可用性与权限代持人校验（避免跨项目/无权限重试人卡死，同时在日志中保留真实重试操作人）
            val parentPipelineInfo = pipelineRepositoryService.getPipelineInfo(
                projectId = event.parentProjectId,
                pipelineId = event.parentPipelineId
            ) ?: return
            if (parentPipelineInfo.locked == true) {
                logger.info("SUB_PIPELINE_RETRY_SYNC|skip|parent pipeline locked|${event.parentPipelineId}")
                return
            }
            val oauthUser = pipelineRepositoryService.getPipelineOauthUser(
                projectId = event.parentProjectId,
                pipelineId = event.parentPipelineId
            ) ?: parentBuildInfo.startUser
            val hasPermission = ChannelContext.withChannel(parentPipelineInfo.channelCode.name) {
                pipelinePermissionService.checkPipelinePermission(
                    userId = oauthUser,
                    projectId = event.parentProjectId,
                    pipelineId = event.parentPipelineId,
                    permission = AuthPermission.EXECUTE
                )
            }
            if (!hasPermission) {
                logger.warn(
                    "SUB_PIPELINE_RETRY_SYNC|skip|no execute permission|oauthUser=$oauthUser|" +
                        "parent=${event.parentProjectId}/${event.parentPipelineId}"
                )
                return
            }

            // 6. 计算本次单步重试后父步骤的实际 executeCount（与 StartBuildContext.init 严格一致）：
            //    已结束单步重试 executeCount+1；运行中局部重试沿用当前 executeCount
            val currentExecuteCount = parentBuildInfo.executeCount ?: 1
            val expectedParentExecuteCount = if (isRunningRetry) currentExecuteCount else currentExecuteCount + 1

            // 7. 写入一次性子构建绑定凭证（在父锁保护下写入；若后续启动抛异常则立即回滚清理）
            subPipelineRetryAttachService.saveAttachInfo(
                SubPipelineRetryAttachService.SubPipelineRetryAttachInfo(
                    parentProjectId = event.parentProjectId,
                    parentPipelineId = event.parentPipelineId,
                    parentBuildId = event.parentBuildId,
                    parentTaskId = event.parentTaskId,
                    expectedParentExecuteCount = expectedParentExecuteCount,
                    subProjectId = event.subProjectId,
                    subPipelineId = event.subPipelineId,
                    subPipelineName = event.subPipelineName,
                    subBuildId = event.subBuildId,
                    subBuildNum = event.subBuildNum,
                    subExecuteCount = event.subExecuteCount,
                    operator = event.operator,
                    triggerTime = System.currentTimeMillis()
                )
            )

            try {
                // 透传多级回溯已访问链路，供父构建自身作为某祖父子流水线时继续安全向上级联
                val extraParams = mapOf(
                    PIPELINE_SUB_PIPELINE_RETRY_SYNC_CHAIN to BuildParameters(
                        key = PIPELINE_SUB_PIPELINE_RETRY_SYNC_CHAIN,
                        value = JsonUtil.toJson(event.visitedBuildIds, formatted = false),
                        valueType = BuildFormPropertyType.TEMPORARY
                    )
                )
                pipelineBuildRetryService.retryWithinLock(
                    userId = oauthUser,
                    projectId = event.parentProjectId,
                    pipelineId = event.parentPipelineId,
                    buildId = event.parentBuildId,
                    taskId = event.parentTaskId,
                    failedContainer = false,
                    skipFailedTask = false,
                    isMobile = false,
                    channelCode = parentBuildInfo.channelCode,
                    preAcquiredStageLock = isRunningRetry,
                    clearAttachOnRetry = false,
                    frequencyLimit = false,
                    extraParamMap = extraParams
                )
                logger.info(
                    "SUB_PIPELINE_RETRY_SYNC|success|parent=${event.parentProjectId}/${event.parentBuildId}|" +
                        "taskId=${event.parentTaskId}|sub=${event.subBuildId}|" +
                        "runningRetry=$isRunningRetry|expectedExecuteCount=$expectedParentExecuteCount|" +
                        "operator=${event.operator}"
                )
            } catch (e: Exception) {
                // 启动失败必须回滚一次性凭证，避免残留影响用户后续手动重试
                subPipelineRetryAttachService.clearTaskAttachInfo(
                    parentProjectId = event.parentProjectId,
                    parentBuildId = event.parentBuildId,
                    parentTaskId = event.parentTaskId
                )
                logger.warn(
                    "SUB_PIPELINE_RETRY_SYNC|retry parent failed|parent=${event.parentBuildId}|" +
                        "taskId=${event.parentTaskId}|sub=${event.subBuildId}|msg=${e.message}",
                    e
                )
            }
        } finally {
            stageLock?.unlock()
            buildLock.unlock()
            needRequeueReason?.let { requeueWithBackoff(event, it) }
        }
    }

    private fun requeueWithBackoff(event: SubPipelineRetrySyncEvent, reason: String) {
        if (event.lockRetryCount >= LOCK_RETRY_BACKOFF_MILLS.size) {
            logger.warn(
                "SUB_PIPELINE_RETRY_SYNC|give up after max lock retries|reason=$reason|" +
                    "parent=${event.parentBuildId}|taskId=${event.parentTaskId}|sub=${event.subBuildId}|" +
                    "lockRetryCount=${event.lockRetryCount}"
            )
            return
        }
        val delay = LOCK_RETRY_BACKOFF_MILLS[event.lockRetryCount]
        logger.info(
            "SUB_PIPELINE_RETRY_SYNC|requeue|reason=$reason|delayMills=$delay|" +
                "parent=${event.parentBuildId}|taskId=${event.parentTaskId}|sub=${event.subBuildId}|" +
                "nextRetry=${event.lockRetryCount + 1}"
        )
        sampleEventDispatcher.dispatch(
            event.copy(
                lockRetryCount = event.lockRetryCount + 1,
                delayMills = delay
            )
        )
    }

    private fun parseVisitedBuildIds(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            JsonUtil.to(raw, object : TypeReference<List<String>>() {})
        }.getOrDefault(emptyList())
    }
}
