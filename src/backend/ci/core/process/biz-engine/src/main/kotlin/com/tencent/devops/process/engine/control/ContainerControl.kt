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

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerCmdChain
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.control.command.container.impl.CheckConditionalSkipContainerCmd
import com.tencent.devops.process.engine.control.command.container.impl.CheckDependOnContainerCmd
import com.tencent.devops.process.engine.control.command.container.impl.CheckDispatchQueueContainerCmd
import com.tencent.devops.process.engine.control.command.container.impl.CheckMutexContainerCmd
import com.tencent.devops.process.engine.control.command.container.impl.CheckPauseContainerCmd
import com.tencent.devops.process.engine.control.command.container.impl.ContainerCmdLoop
import com.tencent.devops.process.engine.control.command.container.impl.InitializeMatrixGroupStageCmd
import com.tencent.devops.process.engine.control.command.container.impl.MatrixExecuteContainerCmd
import com.tencent.devops.process.engine.control.command.container.impl.StartActionTaskContainerCmd
import com.tencent.devops.process.engine.control.command.container.impl.UpdateStateContainerCmdFinally
import com.tencent.devops.process.engine.control.lock.ContainerIdLock
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineAsCodeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 *  Job（运行容器）控制器
 * @version 1.0
 */
@Service
class ContainerControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildVariableService: BuildVariableService,
    private val pipelineAsCodeService: PipelineAsCodeService
) {

    companion object {
        private const val CACHE_SIZE = 500L
        private val LOG = LoggerFactory.getLogger(ContainerControl::class.java)
    }

    private val commandCache: LoadingCache<Class<out ContainerCmd>, ContainerCmd> = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE).build(
            object : CacheLoader<Class<out ContainerCmd>, ContainerCmd>() {
                override fun load(clazz: Class<out ContainerCmd>): ContainerCmd {
                    return SpringContextUtil.getBean(clazz)
                }
            }
        )

    @BkTimed
    fun handle(event: PipelineBuildContainerEvent) {
        val watcher = Watcher(id = "ENGINE|ContainerControl|${event.traceId}|${event.buildId}|Job#${event.containerId}")
        with(event) {
            val containerIdLock = ContainerIdLock(redisOperation, buildId, containerId)
            try {
                containerIdLock.lock()
                watcher.start("execute")
                watcher.start("getContainer")
                val projectId = event.projectId
                // #5951 在已结束或不存在的stage下，不再受理，抛弃消息
                val stage = pipelineStageService.getStage(projectId, buildId, stageId)
                if (stage == null || stage.status.isFinish()) {
                    LOG.warn("ENGINE|$buildId|$source|$stageId|j($containerId)|bad stage with status(${stage?.status})")
                    return
                }
                val container = pipelineContainerService.getContainer(
                    projectId = projectId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId
                ) ?: run {
                    LOG.warn("ENGINE|$buildId|$source|$stageId|j($containerId)|bad container")
                    return
                }
                // 防止关键信息传入错误信息，做一次更正
                val fixEvent = this.copy(
                    stageId = container.stageId,
                    pipelineId = container.pipelineId,
                    containerType = container.containerType,
                    projectId = container.projectId
                )
                container.execute(watcher, fixEvent)
            } finally {
                containerIdLock.unlock()
                watcher.stop()
                LogUtils.printCostTimeWE(watcher = watcher)
            }
        }
    }

    private fun PipelineBuildContainer.execute(watcher: Watcher, event: PipelineBuildContainerEvent) {

        watcher.start("init_context")
        val variables = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
//        val mutexGroup = mutexControl.decorateMutexGroup(controlOption?.mutexGroup, variables)
// 此处迁移到 CheckMutexContainerCmd 类处理更合适
        // 当build的状态是结束的时候，直接返回
//        if (status.isFinish()) {
//            LOG.info("ENGINE|$buildId|${event.source}|$stageId|j($containerId)|status=$status|concurrent")
//            mutexControl.releaseContainerMutex(
//                projectId = projectId,
//                buildId = buildId,
//                stageId = stageId,
//                containerId = containerId,
//                mutexGroup = controlOption?.mutexGroup,
//                executeCount = executeCount
//            )
//            return
//        }

        if (status == BuildStatus.UNEXEC) {
            LOG.warn("ENGINE|UN_EXPECT_STATUS|$buildId|${event.source}|$stageId|j($containerId)|status=$status")
        }

        // 已按任务序号递增排序，如未排序要注意
        val containerTasks = pipelineTaskService.listContainerBuildTasks(projectId, buildId, containerId)
        val executeCount = buildVariableService.getBuildExecuteCount(projectId, pipelineId, buildId)
        val stageMatrixCount = pipelineContainerService.countStageContainers(
            transactionContext = null,
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            onlyMatrixGroup = true
        )
        val pipelineAsCodeEnabled = pipelineAsCodeService.asCodeEnabled(projectId, pipelineId)

        val context = ContainerContext(
            buildStatus = this.status, // 初始状态为容器状态，中间流转会切换状态，并最终赋值给该容器状态
            stageMatrixCount = stageMatrixCount,
            event = event,
            container = this,
            latestSummary = event.reason ?: "init",
            watcher = watcher,
            containerTasks = containerTasks,
            variables = variables,
            pipelineAsCodeEnabled = pipelineAsCodeEnabled,
            executeCount = executeCount
        )

        if (status.isReadyToRun()) {
            context.setUpExt()
        }

        watcher.stop()

        val commandList = listOf(
            commandCache.get(CheckDependOnContainerCmd::class.java), // 检查DependOn依赖处理
            commandCache.get(CheckConditionalSkipContainerCmd::class.java), // 检查条件跳过处理
            commandCache.get(CheckPauseContainerCmd::class.java), // 检查暂停处理
            commandCache.get(CheckMutexContainerCmd::class.java), // 检查Job互斥组处理
            commandCache.get(CheckDispatchQueueContainerCmd::class.java), // 检查流水线全局Job并发队列
            commandCache.get(InitializeMatrixGroupStageCmd::class.java), // 执行matrix运算生成所有Container数据
            commandCache.get(MatrixExecuteContainerCmd::class.java), // 循环进行矩阵执行和状态刷新
            commandCache.get(StartActionTaskContainerCmd::class.java), // 检查启动事件消息
            commandCache.get(ContainerCmdLoop::class.java), // 发送本事件的循环消息
            commandCache.get(UpdateStateContainerCmdFinally::class.java) // 更新Job状态并可能返回Stage处理
        )

        ContainerCmdChain(commandList).doCommand(context)
    }

    private fun ContainerContext.setUpExt() {

        // #7954 初次时解析变量替换真正的Job超时时间
        val timeoutStr = container.controlOption.jobControlOption.timeoutVar?.trim()
        if (!timeoutStr.isNullOrBlank()) {
            val obj = Timeout.decTimeout(timeoutStr, contextMap = variables)
            if (needUpdateControlOption == null) {
                needUpdateControlOption = container.controlOption
            }
            // 替换成真正的超时分钟数int
            needUpdateControlOption?.jobControlOption?.timeout = obj.minutes

            val msg = if (obj.change && obj.replaceByVar) {
                "[SystemLog]Job#${container.seq} " +
                    "reset illegal timeout var[$timeoutStr=${obj.beforeChangeStr}]: ${obj.minutes} minutes"
            } else if (obj.replaceByVar) {
                "[SystemLog]Job#${container.seq} set timeout var[$timeoutStr]: ${obj.minutes} minutes"
            } else if (obj.change) {
                "[SystemLog]Job#${container.seq} reset illegal timeout[$timeoutStr]: ${obj.minutes} minutes"
            } else {
                "[SystemLog]Job#${container.seq} set timeout: ${obj.minutes} minutes"
            }

            buildLogPrinter.addWarnLine(
                buildId = container.buildId,
                message = msg,
                tag = VMUtils.genStartVMTaskId(container.containerId),
                jobId = container.containerHashId ?: "",
                executeCount = executeCount
            )
        } else {
            buildLogPrinter.addDebugLine(
                buildId = container.buildId,
                message = "JobTimeout| Set(${container.controlOption.jobControlOption.timeout}) minutes",
                tag = VMUtils.genStartVMTaskId(container.containerId),
                jobId = container.containerHashId ?: "",
                executeCount = executeCount
            )
        }
    }
}
