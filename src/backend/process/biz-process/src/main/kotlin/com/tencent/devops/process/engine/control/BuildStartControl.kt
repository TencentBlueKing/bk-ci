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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.interceptor.RunLockInterceptor
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.LatestRunningBuild
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildFinishEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStageEvent
import com.tencent.devops.process.engine.pojo.event.PipelineBuildStartEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.service.BuildStartupParamService
import com.tencent.devops.process.service.ProjectOauthTokenService
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PROJECT_NAME
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import redis.clients.jedis.exceptions.JedisException
import java.time.LocalDateTime

/**
 * 构建控制器
 * @version 1.0
 */
@Service
class BuildStartControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildStartupParamService: BuildStartupParamService,
    private val runLockInterceptor: RunLockInterceptor,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val projectOauthTokenService: ProjectOauthTokenService,
    private val buildDetailService: PipelineBuildDetailService
) {

    companion object {
        private const val ExpiredTimeInSeconds: Long = 20
        private val logger = LoggerFactory.getLogger(BuildStartControl::class.java)!!
    }

    fun handle(event: PipelineBuildStartEvent) {
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        val userId = event.userId

        val redisLock = RedisLock(redisOperation, "process.pipeline.build.start.$pipelineId", ExpiredTimeInSeconds)
        try {
            redisLock.lock()
            val buildId = event.buildId
            val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            if (buildInfo == null || BuildStatus.isFinish(buildInfo.status)) {
                logger.info("[$buildId]|BUILD_START| The build is finish!")
                return
            }

            val model = getModel(event, buildInfo)

            // 空节点
            if (model != null) {
                if (model.stages.size == 1) {
                    finish(projectId, pipelineId, userId, buildId)
                } else {
                    event.sendStageStart(model.stages[1].id!!)
                }
            }
        } catch (retryException: JedisException) {
            throw retryException
        } catch (ignored: Throwable) {
            logger.error("[$pipelineId] start fail $ignored", ignored)
        } finally {
            redisLock.unlock()
        }
    }

    private fun PipelineBuildStartEvent.sendStageStart(stageId: String) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildStageEvent(
                source = javaClass.simpleName,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                stageId = stageId,
                actionType = actionType
            )
        )
    }

    private fun getModel(event: PipelineBuildStartEvent, buildInfo: BuildInfo): Model? {

        if (isLock(event, buildInfo)) return null

        val taskId = event.taskId
        val pipelineId = event.pipelineId
        val buildId = event.buildId
        val projectId = event.projectId
        val userId = event.userId
        val model = buildDetailService.getBuildModel(buildId)

        if (model == null) {
            logger.warn("[$pipelineId]| not exist of model of build [$buildId]")
            pipelineEventDispatcher.dispatch(
                PipelineBuildCancelEvent(
                    source = "MODEL_NOT_EXISTS",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildId,
                    status = BuildStatus.UNEXEC
                )
            )
        } else {
            if (BuildStatus.isReadyToRun(buildInfo.status)) {

                updateModel(model = model, buildId = buildId, taskId = taskId)
                // 这入启动参数
                writeStartParam(pipelineId = pipelineId, buildId = buildId, model = model)

                val projectName = projectOauthTokenService.getProjectName(projectId) ?: ""
                val map = mapOf(
                    PIPELINE_BUILD_ID to buildId,
                    PROJECT_NAME to projectId,
                    PROJECT_NAME_CHINESE to projectName,
                    PIPELINE_ID to pipelineId
                )

                pipelineRuntimeService.batchSetVariable(buildId, map)
            }
        }
        return model
    }

    private fun isLock(event: PipelineBuildStartEvent, buildInfo: BuildInfo): Boolean {
        if (BuildStatus.isReadyToRun(buildInfo.status)) {
            val pipelineId = buildInfo.pipelineId
            val buildId = buildInfo.buildId
            val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(pipelineId)
            if (buildSummaryRecord!!.runningCount > 0) {
                if (checkLook(pipelineId, buildId)) {
                    return true
                }
            }

            pipelineRuntimeService.startLatestRunningBuild(
                LatestRunningBuild(
                    projectId = event.projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    userId = buildInfo.startUser,
                    status = BuildStatus.RUNNING,
                    taskCount = buildInfo.taskCount,
                    buildNum = buildInfo.buildNum
                ), retry = event.actionType == ActionType.RETRY
            )
        }
        return false
    }

    private fun checkLook(pipelineId: String, buildId: String): Boolean {
        val setting = pipelineRepositoryService.getSetting(pipelineId)
        if (setting != null) {
            val response = runLockInterceptor.checkRunLock(setting.runLockType, pipelineId)
            if (response.data != BuildStatus.RUNNING) {
                logger.info("[$buildId]|BUILD_IN_QUEUE|response=$response")
                return true
            }
        }
        return false
    }

    private fun finish(projectId: String, pipelineId: String, userId: String, buildId: String) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildFinishEvent(
                source = javaClass.simpleName,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                status = BuildStatus.SUCCEED
            )
        )
    }

    private fun updateModel(model: Model, buildId: String, taskId: String) {
        var find = false
        val now = LocalDateTime.now()
        val stage = model.stages[0]
        val container = stage.containers[0]
        run lit@{
            container.elements.forEach {
                if (it.id == taskId) {
                    pipelineRuntimeService.updateContainerStatus(
                        buildId = buildId,
                        stageId = stage.id!!,
                        containerId = container.id!!,
                        startTime = now,
                        endTime = now,
                        buildStatus = BuildStatus.SUCCEED
                    )
                    it.status = BuildStatus.SUCCEED.name
                    find = true
                    return@lit
                }
            }
        }

        pipelineRuntimeService.updateStage(
            buildId = buildId,
            stageId = stage.id!!,
            startTime = now,
            endTime = now,
            buildStatus = BuildStatus.SUCCEED
        )

        if (find) {
            container.status = BuildStatus.SUCCEED.name
            container.systemElapsed = 0
            container.elementElapsed = 0
            buildDetailService.updateModel(buildId, model)
        }
    }

    private fun writeStartParam(pipelineId: String, buildId: String, model: Model) {
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer

        if (triggerContainer.buildNo != null) {
            val buildNo = pipelineRuntimeService.getBuildNo(pipelineId)
            pipelineRuntimeService.setVariable(buildId, BUILD_NO, buildNo)
        }
        // 写
        if (triggerContainer.params.isNotEmpty()) {
            // 只有在构建参数中的才设置
            val allVariable = pipelineRuntimeService.getAllVariable(buildId)
            val params = allVariable.filter {
                it.key.startsWith(SkipElementUtils.prefix) || it.key == BUILD_NO || it.key == PIPELINE_RETRY_COUNT
            }.plus(triggerContainer.params.map {
                if (allVariable.containsKey(it.id)) { // 做下真实传值的替换
                    it.id to allVariable[it.id]
                } else {
                    it.id to it.defaultValue
                }
            }.toMap())
            buildStartupParamService.addParam(buildId, JsonUtil.getObjectMapper().writeValueAsString(params))
        }
    }
}
