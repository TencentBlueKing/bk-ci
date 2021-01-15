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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.control

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.bean.PipelineUrlBean
import com.tencent.devops.process.engine.common.BS_CONTAINER_END_SOURCE_PREIX
import com.tencent.devops.process.engine.common.BS_MANUAL_START_STAGE
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.lock.StageIdLock
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.util.NotifyTemplateUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 *  步骤控制器
 * @version 1.0
 */
@Service
class StageControl @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildVariableService: BuildVariableService,
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineUrlBean: PipelineUrlBean,
    private val buildLogPrinter: BuildLogPrinter
) {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun handle(event: PipelineBuildStageEvent) {
        val watcher = Watcher(id = "StageControl|${event.traceId}|${event.buildId}|Stage#${event.stageId}")
        with(event) {
            val stageIdLock = StageIdLock(redisOperation, buildId, stageId)
            try {
                watcher.start("lock")
                stageIdLock.lock()
                watcher.start("execute")
                execute()
            } finally {
                stageIdLock.unlock()
                watcher.stop()
                LogUtils.printCostTimeWE(watcher)
            }
        }
    }

    private fun PipelineBuildStageEvent.execute() {

        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            ?: run {
                logger.info("[$buildId]|STAGE_$actionType|s($stageId)| have not build detail!")
                return
            }

        // 当前构建整体的状态，可能是运行中，也可能已经失败
        // 已经结束的构建，不再受理，抛弃消息
        if (BuildStatus.isFinish(buildInfo.status)) {
            logger.info("[$buildId]|STAGE_REPEAT_EVENT|s($stageId)|status=${buildInfo.status}")
            return
        }

        val stages = pipelineStageService.listStages(buildId)

        val stage = stages.filter { it.stageId == stageId }.firstOrNull() // 先过滤，再过滤列表取第1个（唯一）元素
            ?: run {
                logger.warn("[$buildId]|[$source]|s($stageId)|${buildInfo.status}|bad stage")
                return
            }

        val variables = buildVariableService.getAllVariable(buildId)

        val allContainers = pipelineRuntimeService.listContainers(buildId)

        val containerList = allContainers.filter { it.stageId == stageId }

        var buildStatus: BuildStatus

        // 只有在非手动触发该Stage的首次运行做审核暂停
        val needPause = stage.controlOption?.stageControlOption?.manualTrigger == true &&
            source != BS_MANUAL_START_STAGE && stage.controlOption?.stageControlOption?.triggered == false

        val fastKill = isFastKill(stage)

        logger.info("[$buildId]|[$source]|STAGE|s($stageId)|action=$actionType|pause=$needPause|kill=$fastKill")
        // 若stage状态为暂停，且事件类型不是BS_MANUAL_START_STAGE，碰到状态为暂停就停止运行
        if (BuildStatus.isPause(stage.status) && source != BS_MANUAL_START_STAGE) {
            logger.info("[$buildId]|[$source]|STAGE_STOP_BY_PAUSE|s($stageId)|status=${stage.status}|stop pipeline")
            return
        }

        // [终止事件]或[等待审核超时] 直接结束流水线，不需要判断各个Stage的状态，可直接停止
        if (ActionType.isTerminate(actionType) || fastKill) {
            buildStatus = BuildStatus.TERMINATE

            // 如果是[fastKill] 则根据规则指定状态
            if (fastKill) buildStatus = getFastKillStatus(containerList)
            stages.forEach { s ->
                if (s.status.isRunning()) {
                    pipelineStageService.updateStageStatus(
                        buildId = buildId,
                        stageId = s.stageId,
                        buildStatus = buildStatus
                    )
                }
            }
            allContainers.forEach { c ->
                if (c.status.isRunning()) {
                    pipelineRuntimeService.updateContainerStatus(
                        buildId = buildId,
                        stageId = c.stageId,
                        containerId = c.containerId,
                        endTime = LocalDateTime.now(),
                        buildStatus = buildStatus
                    )
                }

                if (fastKill) {
                    buildLogPrinter.addYellowLine(
                        buildId = c.buildId,
                        message = "job(${c.containerId}) stop by fast kill",
                        tag = VMUtils.genStartVMTaskId(c.containerId),
                        jobId = VMUtils.genStartVMTaskId(c.containerId),
                        executeCount = c.executeCount
                    )
                }
            }

            // 如果是因fastKill强制终止，流水线状态标记为失败
            if (fastKill) buildStatus = BuildStatus.FAILED

            // 如果是因审核超时终止构建，流水线状态保持
            pipelineBuildDetailService.updateStageStatus(buildId, stageId = stageId, buildStatus = buildStatus)
            return sendTerminateEvent(source = "fastKill_s($stageId)", buildStatus = buildStatus)
        }

        // 仅在初次进入Stage时进行跳过判断
        if (stage.status.isReadyToRun()) {
            logger.info("[$buildId]|[$source]|STAGE_START|s($stageId)|action=$actionType|status=${stage.status}")

            if (checkIfAllSkip(buildId, stage, containerList, variables)) {
                // 执行条件不满足或未启用该Stage
                logger.info("[$buildId]|[$source]|STAGE_SKIP|s($stageId)|action=$actionType|status=${stage.status}")

                pipelineStageService.skipStage(userId = buildId, buildStage = stage)
                actionType = ActionType.SKIP
            } else if (needPause) {
                // 进入暂停状态等待手动触发
                logger.info("[$buildId]|[$source]|STAGE_PAUSE|s($stageId)|action=$actionType|status=${stage.status}")

                val triggerUsers = stage.controlOption?.stageControlOption?.triggerUsers?.joinToString(",") ?: ""
                val realUsers = EnvUtils.parseEnv(triggerUsers, variables).split(",").toList()
                stage.controlOption!!.stageControlOption.triggerUsers = realUsers
                NotifyTemplateUtils.sendReviewNotify(
                    client = client,
                    projectId = projectId,
                    reviewUrl = pipelineUrlBean.genBuildDetailUrl(projectId, pipelineId, buildId),
                    reviewAppUrl = pipelineUrlBean.genAppBuildDetailUrl(projectId, pipelineId, buildId),
                    reviewDesc = stage.controlOption!!.stageControlOption.reviewDesc ?: "",
                    receivers = realUsers,
                    runVariables = variables
                )
                pipelineStageService.pauseStage(userId = userId, buildStage = stage)
                return
            }
        }

        // 该Stage进入运行状态，若存在审核变量设置则写入环境
        if (stage.controlOption?.stageControlOption?.reviewParams?.isNotEmpty() == true) {
            buildVariableService.batchUpdateVariable(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = stage.controlOption?.stageControlOption?.reviewParams!!
                    .filter { !it.key.isNullOrBlank() }
                    .map { it.key!! to it.value.toString() }.toMap()
            )
        }

        // 执行成功则结束本次事件处理，否则要尝试下一stage
        buildStatus = stage.judgeStageContainer(allContainers = allContainers, actionType = actionType, userId = userId)
        logger.info("[$buildId]|[$source]|STAGE_DONE|s($stageId)|action=$actionType|status=$buildStatus")
        // 如果当前Stage[还未结束]或者[执行失败]或[已经是最后一个]，则不尝试下一Stage
        var gotoNextStage = true
        when {
            buildStatus.isCancel() -> gotoNextStage = false // 当前Stage取消执行，不继续下一个stage
            buildStatus.isRunning() -> gotoNextStage = false // 当前Stage还未结束，不继续下一个stage
            buildStatus.isFailure() -> gotoNextStage = false // 当前Stage失败了，不继续下一个stage
            stage.seq == stages.lastIndex -> gotoNextStage = false // 当前是最后一个Stage，没有下一个stage
        }
        if (gotoNextStage) { // 直接发送执行下一个Stage的消息
            sendStartStageEvent(source = "next_stage_from_s($stageId)", stageId = stages[stage.seq + 1].stageId)
        } else {
            if (buildStatus.isFinish()) { // 构建状态结束了
                pipelineBuildDetailService.updateStageStatus(buildId, stageId = stageId, buildStatus = buildStatus)
                // 如果最后一个stage被跳过，流水线最终设为成功
                if (buildStatus == BuildStatus.SKIP) {
                    buildStatus = BuildStatus.SUCCEED
                }
                sendFinishEvent(source = "finally_s($stageId)", buildStatus = buildStatus)
            }
        }
    }

    /**
     * 判断当前Stage具备下发消息的场景并下发Container消息
     * @param allContainers 所有Job对象
     * @param actionType 事件指令
     * @param userId 执行者
     * @return 返回true表示已经下发，如下发失败则返回false
     */
    private fun PipelineBuildStage.judgeStageContainer(
        allContainers: Collection<PipelineBuildContainer>,
        actionType: ActionType,
        userId: String
    ): BuildStatus {

        var buildStatus = status
        var newActionType = actionType
        // 针对刚启动的Stage
        if (BuildStatus.isReadyToRun(status)) {
            if (actionType == ActionType.REFRESH) { // 对未启动的Stage要变成开始指令
                newActionType = ActionType.START
            }

            when {
                ActionType.isStart(newActionType) -> buildStatus = BuildStatus.RUNNING // 要启动Stage
                ActionType.isEnd(newActionType) -> buildStatus = BuildStatus.CANCELED // 若为终止命令，直接设置为取消
                newActionType == ActionType.SKIP -> buildStatus = BuildStatus.SKIP // 要跳过Stage
            }

            if (buildStatus == BuildStatus.RUNNING) { // 第一次启动，需要初始化状态
                pipelineBuildDetailService.updateStageStatus(buildId, stageId, buildStatus)
                logger.info("[$buildId]|STAGE_INIT|s($stageId)|action=$newActionType")
            }
        } else if (status == BuildStatus.PAUSE && ActionType.isEnd(newActionType)) {
            buildStatus = BuildStatus.STAGE_SUCCESS
        }

        logger.info("[$buildId]|STAGE_$actionType|s($stageId)|action=$newActionType|status=$buildStatus")

        pipelineStageService.updateStageStatus(buildId = buildId, stageId = stageId, buildStatus = buildStatus)

        if (BuildStatus.isFinish(buildStatus) || buildStatus == BuildStatus.STAGE_SUCCESS) {
            return buildStatus // 已经是结束或者是STAGE_SUCCESS就直接返回
        }

        var finishContainers = 0
        var failureContainers = 0
        var cancelContainers = 0
        val containers = mutableListOf<PipelineBuildContainer>()
        allContainers.forEach { c ->
            if (c.stageId == stageId) {
                containers.add(c)
                if (BuildStatus.isFinish(c.status)) {
                    finishContainers++
                }
                if (BuildStatus.isCancel(c.status)) {
                    cancelContainers++
                }
                if (BuildStatus.isFailure(c.status)) {
                    failureContainers++
                }
            }
        }

        if (finishContainers < containers.size) { // 还有未执行完的任务,继续下发其他构建容器
            sendContainerEvent(containers = containers, actionType = newActionType, userId = userId)
        } else if (finishContainers == containers.size && !BuildStatus.isFinish(status)) { // 全部执行完且Stage状态不是已完成
            buildStatus = if (failureContainers == 0 && cancelContainers == 0) {
                BuildStatus.SUCCEED
            } else if (failureContainers > 0) {
                BuildStatus.FAILED
            } else if (cancelContainers > 0) {
                BuildStatus.CANCELED
            } else {
                BuildStatus.FAILED
            }
            pipelineStageService.updateStageStatus(buildId, stageId, buildStatus)
            pipelineBuildDetailService.updateStageStatus(buildId, stageId, buildStatus)
            logger.info("[$buildId]|STAGE_CONTAINER_FINISH|s($stageId)|status=$buildStatus|action=$actionType")
        }
        return buildStatus
    }

    fun checkIfAllSkip(
        buildId: String,
        stage: PipelineBuildStage,
        containerList: Collection<PipelineBuildContainer>,
        variables: Map<String, Any>
    ): Boolean {

        val stageId = stage.stageId
        val stageControlOption = stage.controlOption?.stageControlOption
        if (stageControlOption?.enable == false || containerList.isEmpty()) {
            logger.info("[$buildId]|STAGE_SKIP|s($stageId)|enable=${stageControlOption?.enable}")
            return true
        }

        var skip = false
        if (stageControlOption != null) {
            val conditions = stageControlOption.customVariables ?: emptyList()
            skip = ControlUtils.checkStageSkipCondition(conditions, variables, buildId, stageControlOption.runCondition)
        }
        if (skip) {
            logger.info("[$buildId]|STAGE_CONDITION_SKIP|s($stageId)|conditions=$stageControlOption")
        }
        return skip
    }

    private fun PipelineBuildStageEvent.sendTerminateEvent(source: String, buildStatus: BuildStatus) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildCancelEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                status = buildStatus
            )
        )
    }

    private fun PipelineBuildStageEvent.sendFinishEvent(source: String, buildStatus: BuildStatus) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                status = buildStatus
            )
        )
    }

    /**
     * 下发容器构建事件
     * @param containers 当前container对象列表
     * @param actionType 事件指令
     * @param userId 执行者
     * @return 返回true表示已经下发，如下发失败则返回false
     */
    private fun PipelineBuildStage.sendContainerEvent(
        containers: Collection<PipelineBuildContainer>,
        actionType: ActionType,
        userId: String
    ) {
        // 同一Stage下的多个Container是并行
        containers.forEach nextContainer@{ container ->
            val containerStatus = container.status
            val containerId = container.containerId
            logger.info("[$buildId]|s($stageId)|j($containerId)|status=$containerStatus")
            if (BuildStatus.isFinish(containerStatus)) {
                return@nextContainer // 已经执行完毕的直接跳过
            } else if (BuildStatus.isReadyToRun(containerStatus) && !ActionType.isStart(actionType)) {
                return@nextContainer // 失败或可重试的容器，如果不是重试动作，则跳过
            } else if (BuildStatus.isRunning(containerStatus) && !ActionType.isTerminate(actionType)) {
                return@nextContainer // 已经在运行中的, 只接受强制终止
            }

            container.sendBuildContainerEvent(actionType = actionType, userId = userId)
            logger.info("[$buildId]|STAGE_CONTAINER_$actionType|s($stageId)|j($containerId)")
        }
    }

    private fun PipelineBuildContainer.sendBuildContainerEvent(actionType: ActionType, userId: String) {
        // 通知容器构建消息
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "start_container_from_s($stageId)",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                stageId = stageId,
                containerType = containerType,
                containerId = containerId,
                actionType = actionType
            )
        )
    }

    private fun PipelineBuildStageEvent.sendStartStageEvent(source: String, stageId: String) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildStageEvent(
                source = source,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                stageId = stageId,
                actionType = ActionType.START
            )
        )
    }

    private fun PipelineBuildStageEvent.isFastKill(stage: PipelineBuildStage): Boolean {
        var kill = false
        if (stage.controlOption?.fastKill == true) {
            if (source == "$BS_CONTAINER_END_SOURCE_PREIX${BuildStatus.FAILED}" ||
                source == "$BS_CONTAINER_END_SOURCE_PREIX${BuildStatus.CANCELED}"
            ) {
                kill = true
            }
        }
        return kill
    }

    private fun PipelineBuildStageEvent.getFastKillStatus(containerRecords: List<PipelineBuildContainer>): BuildStatus {
        // fastKill状态下：stage内有失败的状态优先取失败状态。若有因插件暂停导致的cancel状态，在没有fail状态情况下，取cancel状态，有fail取fail状态
        var buildStatus = BuildStatus.FAILED
        val pauseStop = containerRecords.filter { BuildStatus.isCancel(it.status) }
        if (pauseStop.isEmpty()) {
            logger.info("[$buildId]|FAST_KILL_PAUSE|s($stageId)|fastKill no pauseAtom")
        } else {
            // 若有暂停的container需要判断该stage下是否有失败的container，若有失败的则为失败，否则就为取消
            var failCount = 0
            containerRecords.forEach {
                if (BuildStatus.isFailure(it.status)) {
                    failCount++
                }
            }
            if (failCount == 0) {
                logger.info("[$buildId]|FAST_KILL_PAUSE|s($stageId)|have pauseAtom|status=${BuildStatus.CANCELED}")
                buildStatus = BuildStatus.CANCELED
            }
        }
        return buildStatus
    }
}
