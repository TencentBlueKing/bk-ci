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

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStartBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStatusBroadCastEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.ProcessMessageCode.BK_START_USER
import com.tencent.devops.process.constant.ProcessMessageCode.BK_TRIGGER_USER
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_QUEUE_FOR_CONCURRENCY
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_QUEUE_FOR_SINGLE
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.ConcurrencyGroupLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildNoLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildStartLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRepositoryVersionService
import com.tencent.devops.process.engine.service.PipelineRuntimeExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.engine.service.record.StageBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.PIPELINE_TIME_START
import com.tencent.devops.process.utils.PipelineVarUtil
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.max

/**
 * 构建控制器
 * @version 1.0
 */
@Service
@Suppress("TooManyFunctions", "LongParameterList", "ReturnCount", "NestedBlockDepth")
class BuildStartControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineRepositoryVersionService: PipelineRepositoryVersionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val buildDetailService: PipelineBuildDetailService,
    private val pipelineRecordService: PipelineBuildRecordService,
    private val stageRecordService: StageBuildRecordService,
    private val containerRecordService: ContainerBuildRecordService,
    private val taskRecordService: TaskBuildRecordService,
    private val buildVariableService: BuildVariableService,
    private val scmProxyService: ScmProxyService,
    private val buildLogPrinter: BuildLogPrinter,
    private val meterRegistry: MeterRegistry,
    private val pipelineUrlBean: PipelineUrlBean
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildStartControl::class.java)!!
        private const val TAG = "startVM-0"
        private const val JOB_ID = "0"
        private const val DEFAULT_DELAY = 1000
    }

    @BkTimed
    fun handle(event: PipelineBuildStartEvent) {
        val watcher = Watcher(id = "ENGINE|BuildStart|${event.traceId}|${event.buildId}|${event.status}")
        with(event) {
            try {
                execute(watcher)
            } catch (ignored: Throwable) {
                LOG.error("ENGINE|$buildId|$source| start fail $ignored", ignored)
            } finally {
                watcher.stop()
                LogUtils.printCostTimeWE(watcher = watcher)
            }
        }
    }

    private fun PipelineBuildStartEvent.retry() {
        LOG.info("ENGINE|$buildId|$source|RETRY_TO_LOCK")
        this.delayMills = DEFAULT_DELAY
        pipelineEventDispatcher.dispatch(this)
    }

    fun PipelineBuildStartEvent.execute(watcher: Watcher) {
        val executeCount = buildVariableService.getBuildExecuteCount(projectId, pipelineId, buildId)
        buildLogPrinter.addDebugLine(
            buildId = buildId, message = "Enter BuildStartControl",
            tag = TAG, jobId = JOB_ID, executeCount = executeCount
        )

        watcher.start("pickUpReadyBuild")
        val buildInfo = pickUpReadyBuild(executeCount = executeCount) ?: run {
            return
        }
        watcher.stop()

        watcher.start("buildModel")
        buildModel(buildInfo = buildInfo, executeCount = executeCount)
        watcher.stop()

        buildLogPrinter.addDebugLine(
            buildId = buildId, message = "BuildStartControl End",
            tag = TAG, jobId = JOB_ID, executeCount = executeCount
        )

        buildLogPrinter.stopLog(buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount)
        startPipelineCount()
    }

    private fun PipelineBuildStartEvent.pickUpReadyBuild(executeCount: Int): BuildInfo? {

        val buildIdLock = BuildIdLock(redisOperation = redisOperation, buildId = buildId)
        return try {
            buildIdLock.lock()
            val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            if (buildInfo == null || buildInfo.status.isFinish() || buildInfo.status.isNeverRun()) {
                buildLogPrinter.addLine(
                    message = "Stop #${buildInfo?.buildNum} ${buildInfo?.status}",
                    buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
                )
                LOG.info("ENGINE|$buildId][$source|BUILD_START_DONE|status=${buildInfo?.status}")
                null
            } else if (tryToStartRunBuild(buildInfo, executeCount = executeCount)) {
                buildInfo
            } else {
                null
            }
        } finally {
            buildIdLock.unlock()
        }
    }

    @Suppress("LongMethod", "NestedBlockDepth")
    private fun PipelineBuildStartEvent.tryToStartRunBuild(buildInfo: BuildInfo, executeCount: Int): Boolean {
        LOG.info("ENGINE|$buildId|$source|BUILD_START|${buildInfo.status}")
        var canStart = true
        // 已经是启动状态的，直接返回
        if (!buildInfo.status.isReadyToRun()) {
            buildLogPrinter.addLine(
                message = "Illegal build #${buildInfo.buildNum} [${buildInfo.status}]",
                buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
            )
            return false
        }
        val pipelineBuildLock = PipelineBuildStartLock(redisOperation, pipelineId)
        try {
            if (!pipelineBuildLock.tryLock()) {
                retry()
                return false
            }
            val setting = pipelineRepositoryService.getSetting(projectId, pipelineId)
            // #4074 LOCK 不会进入到这里，在启动API已经拦截
            if (setting?.runLockType == PipelineRunLockType.SINGLE ||
                setting?.runLockType == PipelineRunLockType.SINGLE_LOCK
            ) {
                canStart = checkSingleType(
                    buildInfo = buildInfo,
                    setting = setting,
                    executeCount = executeCount
                )
            }

            if (setting?.runLockType == PipelineRunLockType.GROUP_LOCK) {
                canStart = checkGroupType(
                    buildInfo = buildInfo,
                    setting = setting,
                    executeCount = executeCount
                )
            }

            if (canStart) {
                buildLogPrinter.addLine(
                    message = "Build #${buildInfo.buildNum} preparing",
                    buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
                )
                handleBuildNo(buildInfo)
                pipelineRuntimeService.startLatestRunningBuild(
                    latestRunningBuild = LatestRunningBuild(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        userId = buildInfo.startUser,
                        status = BuildStatus.RUNNING,
                        taskCount = buildInfo.taskCount,
                        buildNum = buildInfo.buildNum
                    ),
                    executeCount = executeCount
                )
                broadcastStartEvent(buildInfo)
            } else {
                pipelineRuntimeService.updateExecuteCount(
                    projectId = projectId,
                    buildId = buildId,
                    executeCount = executeCount
                )
            }
        } finally {
            pipelineBuildLock.unlock()
        }
        return canStart
    }

    private fun PipelineBuildStartEvent.checkGroupType(
        buildInfo: BuildInfo,
        setting: PipelineSetting,
        executeCount: Int
    ): Boolean {
        var checkStart = true
        val concurrencyGroup = buildInfo.concurrencyGroup ?: pipelineId
        ConcurrencyGroupLock(redisOperation, projectId, concurrencyGroup).use { groupLock ->
            groupLock.lock()
            if (buildInfo.status != BuildStatus.QUEUE_CACHE) {
                // 只有最新进来排队的构建才能QUEUE -> QUEUE_CACHE
                checkStart = pipelineRuntimeExtService.popNextConcurrencyGroupQueueCanPend2Start(
                    projectId = projectId,
                    concurrencyGroup = concurrencyGroup,
                    buildId = buildId
                )?.buildId == buildId
            }
            // #6521 并发组中需要等待其他流水线
            val concurrencyGroupRunning = pipelineRuntimeService.getBuildInfoListByConcurrencyGroup(
                projectId = projectId,
                concurrencyGroup = concurrencyGroup,
                status = listOf(BuildStatus.RUNNING)
            ).toMutableList()

            // #8143 兼容旧流水线版本 TODO 待模板设置补上漏洞，后期下掉 #8143
            if (concurrencyGroup == pipelineId) {
                concurrencyGroupRunning.addAll(
                    0,
                    pipelineRuntimeService.getBuildInfoListByConcurrencyGroupNull(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        status = listOf(BuildStatus.RUNNING)
                    )
                )
            }

            LOG.info("ENGINE|$buildId|$source|CHECK_GROUP_TYPE|$concurrencyGroup|${concurrencyGroupRunning.count()}")
            if (concurrencyGroupRunning.isNotEmpty()) {
                // 需要重新入队等待
                pipelineRuntimeService.updateBuildInfoStatus2Queue(
                    projectId = projectId,
                    buildId = buildId,
                    oldStatus = BuildStatus.QUEUE_CACHE,
                    showMsg = I18nUtil.getCodeLanMessage(
                        messageCode = BUILD_QUEUE_FOR_CONCURRENCY,
                        defaultMessage = "QUEUE: concurrency for group($concurrencyGroup)",
                        params = arrayOf(concurrencyGroup)
                    )
                )
                val detailUrl = pipelineUrlBean.genBuildDetailUrl(
                    projectCode = projectId,
                    pipelineId = concurrencyGroupRunning.first().first,
                    buildId = concurrencyGroupRunning.first().second,
                    position = null,
                    stageId = null,
                    needShortUrl = false
                )
                buildLogPrinter.addLine(
                    message = "Mode: ${setting.runLockType}," +
                        "concurrency for group($concurrencyGroup) " +
                        "and queue: ${concurrencyGroupRunning.count()}, now waiting for " +
                        "<a target='_blank' href='$detailUrl'>${concurrencyGroupRunning.first().second}</a>",
                    buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
                )
                checkStart = false
            }
        }
        return checkStart
    }

    private fun PipelineBuildStartEvent.checkSingleType(
        buildInfo: BuildInfo,
        setting: PipelineSetting,
        executeCount: Int
    ): Boolean {
        // #4074 锁定当前构建是队列中第一个排队待执行的
        var checkStart = true
        if (buildInfo.status != BuildStatus.QUEUE_CACHE) {
            checkStart = pipelineRuntimeExtService.queueCanPend2Start(projectId, pipelineId, buildId = buildId)
        }
        if (checkStart) {
            val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(projectId, pipelineId)

            if (buildSummaryRecord!!.runningCount > 0) {
                // 需要重新入队等待
                pipelineRuntimeService.updateBuildInfoStatus2Queue(
                    projectId = projectId, buildId = buildId, oldStatus = BuildStatus.QUEUE_CACHE,
                    showMsg = I18nUtil.getCodeLanMessage(
                        messageCode = BUILD_QUEUE_FOR_SINGLE,
                        defaultMessage = "QUEUE: The current build is queued"
                    )
                )

                buildLogPrinter.addLine(
                    message = "Mode: ${setting.runLockType}, queue: ${buildSummaryRecord.runningCount}",
                    buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
                )
                checkStart = false
            }
        } else {
            buildLogPrinter.addLine(
                message = "Waiting build #${buildInfo.buildNum - 1}",
                buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
            )
        }
        return checkStart
    }

    /**
     * 防止"每次构建成功+1"读取到相同buildNo的情况：在排队过程中，前面的构建成功结束了，会加1，所以正式启动前设置最新的buildNo
     *
     * 注：重试不会执行
     */
    private fun PipelineBuildStartEvent.handleBuildNo(buildInfo: BuildInfo) {
        val retryFlag = buildInfo.executeCount?.let { it > 1 } == true || buildInfo.retryFlag == true
        if (retryFlag || buildNoType != BuildNoType.SUCCESS_BUILD_INCREMENT) { // 重试不重新写
            return
        }
        // 防止"每次构建成功+1"读取到相同buildNo的情况：在排队过程中，前面的构建成功结束了，会加1，所以正式启动前设置最新的buildNo
        PipelineBuildNoLock(redisOperation = redisOperation, pipelineId = pipelineId).use { buildNoLock ->
            buildNoLock.lock()

            val buildNo = pipelineRuntimeService.getBuildSummaryRecord(projectId, pipelineId = pipelineId)?.buildNo

            if (buildNo != null) {
                buildVariableService.setVariable(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    varName = BUILD_NO,
                    varValue = buildNo
                )

                var parameters = pipelineRuntimeService.getBuildParametersFromStartup(projectId, buildId = buildId)
                val startParamMap = mutableMapOf<String, BuildParameters>()
                parameters.associateByTo(startParamMap) { it.key }
                startParamMap[BUILD_NO] = BuildParameters(key = BUILD_NO, value = buildNo, readOnly = true)
                parameters = startParamMap.values.toList()

                val recommendVersionPrefix = PipelineVarUtil.getRecommendVersionPrefix(parameters)
                if (recommendVersionPrefix != null) {
                    pipelineRuntimeService.updateRecommendVersion(
                        projectId = projectId,
                        buildId = buildId,
                        recommendVersion = "$recommendVersionPrefix.$buildNo"
                    )
                }

                pipelineRuntimeService.updateBuildParameters(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    buildParameters = parameters
                )
            }
        }
    }

    private fun PipelineBuildStartEvent.broadcastStartEvent(buildInfo: BuildInfo) {
        pipelineEventDispatcher.dispatch(
            // 广播构建即将启动消息给订阅者
            PipelineBuildStartBroadCastEvent(
                source = TAG,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = buildInfo.startUser,
                buildId = buildId,
                startTime = buildInfo.startTime,
                triggerType = buildInfo.trigger
            ),
            // 根据状态做响应的扩展广播消息给订阅者
            PipelineBuildStatusBroadCastEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                actionType = ActionType.START
            )
        )
    }

    private fun updateModel(model: Model, buildInfo: BuildInfo, taskId: String, executeCount: Int) {
        val now = LocalDateTime.now()
        val stage = model.stages[0]
        val container = stage.containers[0]
        run lit@{
            container.name = ContainerUtils.getClearedQueueContainerName(container.name)
            container.elements.forEach {
                if (it.id == taskId) {
                    pipelineContainerService.updateContainerStatus(
                        projectId = buildInfo.projectId,
                        buildId = buildInfo.buildId,
                        stageId = stage.id!!,
                        containerId = container.containerId!!,
                        startTime = now,
                        endTime = now,
                        buildStatus = BuildStatus.SUCCEED
                    )
                    taskRecordService.updateTaskStatus(
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        buildId = buildInfo.buildId,
                        stageId = stage.id!!,
                        containerId = container.containerId!!,
                        taskId = taskId,
                        buildStatus = BuildStatus.SUCCEED,
                        executeCount = executeCount,
                        operation = "updateTriggerElement#$taskId"
                    )
                    it.status = BuildStatus.SUCCEED.name
                    buildLogPrinter.stopLog(buildInfo.buildId, taskId, jobId = JOB_ID, executeCount)
                    return@lit
                }
            }
        }

        pipelineStageService.updateStageStatus(
            projectId = buildInfo.projectId,
            buildId = buildInfo.buildId,
            stageId = stage.id!!,
            buildStatus = BuildStatus.SUCCEED,
            checkIn = stage.checkIn,
            checkOut = stage.checkOut
        )
        pipelineRecordService.updateModelRecord(
            projectId = buildInfo.projectId, pipelineId = buildInfo.pipelineId, buildId = buildInfo.buildId,
            executeCount = executeCount, buildStatus = null, modelVar = mutableMapOf(),
            timestamps = mapOf(
                BuildTimestampType.BUILD_CONCURRENCY_QUEUE to
                    BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
            ),
            startTime = LocalDateTime.now(), endTime = null
        )
        stageRecordService.updateStageRecord(
            projectId = buildInfo.projectId, pipelineId = buildInfo.pipelineId, buildId = buildInfo.buildId,
            stageId = stage.id!!, executeCount = executeCount, buildStatus = BuildStatus.SUCCEED,
            stageVar = mutableMapOf(
                Stage::elapsed.name to max(0, System.currentTimeMillis() - buildInfo.queueTime)
            )
        )
        val nowMills = now.timestampmilli()
        stage.status = BuildStatus.SUCCEED.name
        stage.elapsed = max(0, nowMills - buildInfo.queueTime)
        container.status = BuildStatus.SUCCEED.name
        container.startEpoch = nowMills
        container.systemElapsed = stage.elapsed // 修复可能导致负数的情况
        container.elementElapsed = 0
        container.executeCount = executeCount
        container.startVMStatus = BuildStatus.SUCCEED.name
        containerRecordService.updateContainerRecord(
            projectId = buildInfo.projectId, pipelineId = buildInfo.pipelineId, buildId = buildInfo.buildId,
            executeCount = executeCount, containerId = container.containerId!!, buildStatus = BuildStatus.SUCCEED,
            containerVar = mutableMapOf(
                Container::startEpoch.name to nowMills,
                Container::systemElapsed.name to (stage.elapsed ?: 0),
                Container::elementElapsed.name to 0,
                Container::startVMStatus.name to BuildStatus.SUCCEED.name,
                Container::name.name to container.name, // 名字刷新成非队列中
                Container::timeCost.name to BuildRecordTimeCost()
            )
        )

        buildDetailService.updateModel(projectId = buildInfo.projectId, buildId = buildInfo.buildId, model = model)
        buildLogPrinter.addLine(
            message = I18nUtil.getCodeLanMessage(
                messageCode = BK_TRIGGER_USER,
                language = I18nUtil.getDefaultLocaleLanguage()
            ) + ": ${buildInfo.triggerUser}, " +
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_START_USER,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + ": ${buildInfo.startUser}",
            buildId = buildInfo.buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
        )
    }

    @Suppress("ALL")
    @Deprecated("后台内置插件特殊处理，后续不再维护")
    private fun supplementModel(
        projectId: String,
        pipelineId: String,
        fullModel: Model,
        startParams: MutableMap<String, String>
    ) {
        val variables = mutableMapOf<String, String>()
        fullModel.stages.forEach { stage ->
            stage.containers.forEach nextContainer@{ container ->
                if (container is TriggerContainer) {
                    // 解析变量
                    container.params.forEach { param ->
                        if (startParams[param.id] != null) {
                            variables[param.id] = startParams[param.id].toString()
                        } else {
                            variables[param.id] = param.defaultValue.toString()
                        }
                    }
                    return@nextContainer
                }
                var callScm = true
                container.elements.forEach nextElement@{ ele ->
                    if (!ele.isElementEnable()) {
                        return@nextElement
                    }
                    if (!ele.status.isNullOrBlank()) {
                        val eleStatus = BuildStatus.valueOf(ele.status!!)
                        if (eleStatus.isFinish() && eleStatus != BuildStatus.SKIP) {
                            callScm = false
                            ele.status = ""
                            ele.elapsed = null
                            return@nextElement
                        }
                    }

                    val (repositoryConfig: RepositoryConfig, branchName: String?) =
                        when (ele) {
                            is CodeSvnElement -> {
                                if (ele.revision.isNullOrBlank()) {
                                    RepositoryConfigUtils.buildConfig(ele) to ele.svnPath
                                } else {
                                    return@nextElement
                                }
                            }

                            is CodeGitElement -> {
                                val branchName = when {
                                    ele.gitPullMode != null -> {
                                        if (ele.gitPullMode!!.type != GitPullModeType.COMMIT_ID) {
                                            EnvUtils.parseEnv(ele.gitPullMode!!.value, variables)
                                        } else {
                                            return@nextElement
                                        }
                                    }

                                    !ele.branchName.isNullOrBlank() -> EnvUtils.parseEnv(ele.branchName!!, variables)
                                    else -> return@nextElement
                                }
                                RepositoryConfigUtils.buildConfig(ele) to branchName
                            }

                            is CodeGitlabElement -> {
                                val branchName = when {
                                    ele.gitPullMode != null -> {
                                        if (ele.gitPullMode!!.type != GitPullModeType.COMMIT_ID) {
                                            EnvUtils.parseEnv(ele.gitPullMode!!.value, variables)
                                        } else {
                                            return@nextElement
                                        }
                                    }

                                    !ele.branchName.isNullOrBlank() -> EnvUtils.parseEnv(ele.branchName!!, variables)
                                    else -> return@nextElement
                                }
                                RepositoryConfigUtils.buildConfig(ele) to branchName
                            }

                            is GithubElement -> {
                                val branchName = when {
                                    ele.gitPullMode != null -> {
                                        if (ele.gitPullMode!!.type != GitPullModeType.COMMIT_ID) {
                                            EnvUtils.parseEnv(ele.gitPullMode!!.value, variables)
                                        } else {
                                            return@nextElement
                                        }
                                    }

                                    else -> return@nextElement
                                }
                                RepositoryConfigUtils.buildConfig(ele) to branchName
                            }

                            else -> return@nextElement
                        }

                    if (callScm) {
                        val latestRevision =
                            scmProxyService.recursiveFetchLatestRevision(
                                projectId = projectId,
                                pipelineId = pipelineId,
                                repositoryConfig = repositoryConfig,
                                branchName = branchName,
                                variables = variables,
                                retry = 0
                            )
                        if (latestRevision.isOk() && latestRevision.data != null) {
                            when (ele) {
                                is CodeSvnElement -> {
                                    ele.revision = latestRevision.data!!.revision
                                    ele.specifyRevision = true
                                }

                                is CodeGitElement -> ele.revision = latestRevision.data!!.revision
                                is GithubElement -> ele.revision = latestRevision.data!!.revision
                                is CodeGitlabElement -> ele.revision = latestRevision.data!!.revision
                                else -> return@nextElement
                            }
                        }
                    }
                }
            }
        }
    }

    private fun PipelineBuildStartEvent.buildModel(buildInfo: BuildInfo, executeCount: Int) {
        val model = buildDetailService.getBuildModel(projectId, buildId) ?: run {
            pipelineEventDispatcher.dispatch(
                PipelineBuildCancelEvent(
                    source = TAG, projectId = projectId, pipelineId = pipelineId,
                    userId = userId, buildId = buildId, status = BuildStatus.UNEXEC
                )
            )
            return // model不存在直接取消构建
        }

        // 单步重试不做操作，手动重试需还原各节点状态，启动需获取revision信息
        buildLogPrinter.addLine(
            message = "Async fetch latest commit/revision, please wait...",
            buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
        )
        val startParams: Map<String, String> by lazy {
            buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        }
        if (actionType == ActionType.START) {
            supplementModel(
                projectId = projectId, pipelineId = pipelineId,
                fullModel = model, startParams = startParams as MutableMap<String, String>
            )
        }

        if (buildInfo.status.isReadyToRun()) {
            buildLogPrinter.addLine(
                message = "Updating model & start parameters & variables",
                buildId = buildId, tag = TAG, jobId = JOB_ID, executeCount = executeCount
            )
            updateModel(model = model, buildInfo = buildInfo, taskId = taskId, executeCount = executeCount)
            buildVariableService.setVariable(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                varName = PIPELINE_TIME_START,
                varValue = System.currentTimeMillis().toString()
            )
            // 增加Model版本引用计数
            pipelineRepositoryVersionService.addVerRef(buildInfo.projectId, buildInfo.pipelineId, buildInfo.version)
        }

        val stages = model.stages
        val firstValidStage = getFirstValidStage(stages)
        if (stages.size == 1 || firstValidStage == null) { // 空节点或者没有可用的stage
            pipelineEventDispatcher.dispatch(
                PipelineBuildFinishEvent(
                    source = TAG,
                    projectId = projectId, pipelineId = pipelineId, userId = userId,
                    buildId = buildId, status = BuildStatus.SUCCEED
                )
            )
        } else { // 对第一个可用的Stage下发指令
            pipelineEventDispatcher.dispatch(
                PipelineBuildStageEvent(
                    source = TAG,
                    projectId = projectId, pipelineId = pipelineId, userId = userId,
                    buildId = buildId, stageId = firstValidStage.id!!, actionType = actionType
                )
            )
        }
    }

    private fun getFirstValidStage(stages: List<Stage>): Stage? {
        var firstValidStage: Stage? = null
        val stageSize = stages.size
        if (stageSize == 1) {
            return null
        }
        val endIndex = stageSize - 1
        for (i in 1..endIndex) {
            val stage = stages[i]
            if (stage.stageControlOption?.enable != false) {
                firstValidStage = stage
                break
            }
        }
        return firstValidStage
    }

    private fun startPipelineCount() {
        Counter
            .builder("start_pipeline_count")
            .register(meterRegistry)
            .increment()
    }
}
