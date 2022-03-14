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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageCmdChain
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.control.command.stage.impl.CheckConditionalSkipStageCmd
import com.tencent.devops.process.engine.control.command.stage.impl.CheckInterruptStageCmd
import com.tencent.devops.process.engine.control.command.stage.impl.CheckPauseReviewStageCmd
import com.tencent.devops.process.engine.control.command.stage.impl.StartContainerStageCmd
import com.tencent.devops.process.engine.control.command.stage.impl.UpdateStateForStageCmdFinally
import com.tencent.devops.process.engine.control.lock.StageIdLock
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineContextService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 *  步骤控制器
 * @version 1.0
 */
@Service
class StageControl @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineContainerService: PipelineContainerService,
    private val buildVariableService: BuildVariableService,
    private val pipelineContextService: PipelineContextService,
    private val pipelineStageService: PipelineStageService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(StageControl::class.java)
        private const val CACHE_SIZE = 500L
    }

    private val commandCache: LoadingCache<Class<out StageCmd>, StageCmd> = CacheBuilder.newBuilder()
        .maximumSize(CACHE_SIZE).build(
            object : CacheLoader<Class<out StageCmd>, StageCmd>() {
                override fun load(clazz: Class<out StageCmd>): StageCmd {
                    return SpringContextUtil.getBean(clazz)
                }
            }
        )

    fun handle(event: PipelineBuildStageEvent) {
        val watcher = Watcher(id = "ENGINE|StageControl|${event.traceId}|${event.buildId}|Stage#${event.stageId}")
        with(event) {
            val stageIdLock = StageIdLock(redisOperation, buildId, stageId)
            try {
                watcher.start("lock")
                stageIdLock.lock()
                watcher.start("execute")
                execute(watcher = watcher)
            } finally {
                stageIdLock.unlock()
                watcher.stop()
                LogUtils.printCostTimeWE(watcher)
            }
        }
    }

    private fun PipelineBuildStageEvent.execute(watcher: Watcher) {
        watcher.start("init_context")
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        // 已经结束的构建，不再受理，抛弃消息
        if (buildInfo == null || buildInfo.status.isFinish()) {
            LOG.info("ENGINE|$buildId|$source|STAGE_REPEAT_EVENT|$stageId|${buildInfo?.status}")
            return
        }
        val stage = pipelineStageService.getStage(projectId, buildId, stageId)
            ?: run {
                LOG.warn("ENGINE|$buildId|$source|BAD_STAGE|$stageId|${buildInfo.status}")
                return
            }
        val variables = buildVariableService.getAllVariable(projectId, buildId)
        val containers = pipelineContainerService.listContainers(
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containsMatrix = false
        )
        val executeCount = buildVariableService.getBuildExecuteCount(projectId, buildId)
        val stageContext = StageContext(
            buildStatus = stage.status, // 初始状态为Stage状态，中间流转会切换状态，并最终赋值Stage状态
            event = this,
            stage = stage,
            containers = containers,
            latestSummary = "init",
            watcher = watcher,
            variables = pipelineContextService.getAllBuildContext(variables), // 传递全量上下文
            executeCount = executeCount
        )
        watcher.stop()

        val commandList = listOf<StageCmd>(
            commandCache.get(CheckInterruptStageCmd::class.java), // 快速失败或者中断执行的检查
            commandCache.get(CheckConditionalSkipStageCmd::class.java), // 检查Stage条件跳过处理
            commandCache.get(CheckPauseReviewStageCmd::class.java), // Stage暂停&审核事件处理
            commandCache.get(StartContainerStageCmd::class.java), // 正常执行下发Container事件的处理
            commandCache.get(UpdateStateForStageCmdFinally::class.java) // 最终处理Stage状态和后续事件
        )

        StageCmdChain(commandList).doCommand(stageContext)
    }
}
