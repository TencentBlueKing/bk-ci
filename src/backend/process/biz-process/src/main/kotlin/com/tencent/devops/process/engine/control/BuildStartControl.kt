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

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildStartBroadCastEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.engine.cfg.ModelStageIdGenerator
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildStartLock
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
import com.tencent.devops.process.service.PipelineUserService
import com.tencent.devops.process.service.ProjectOauthTokenService
import com.tencent.devops.process.service.scm.ScmService
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_CREATE_USER
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_TIME_START
import com.tencent.devops.process.utils.PIPELINE_UPDATE_USER
import com.tencent.devops.process.utils.PROJECT_NAME
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 构建控制器
 * @version 1.0
 */
@Service
class BuildStartControl @Autowired constructor(
    private val modelStageIdGenerator: ModelStageIdGenerator,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val buildStartupParamService: BuildStartupParamService,
    private val runLockInterceptor: RunLockInterceptor,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val projectOauthTokenService: ProjectOauthTokenService,
    private val buildDetailService: PipelineBuildDetailService,
    private val pipelineUserService: PipelineUserService,
    private val scmService: ScmService,
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(javaClass)!!

    private val tag = "buildStartControl"

    fun handle(event: PipelineBuildStartEvent) {
        with(event) {
            val pipelineBuildLock = PipelineBuildStartLock(redisOperation, pipelineId)
            try {
                pipelineBuildLock.lock()
                execute()
            } catch (e: Throwable) {
                logger.error("[$buildId]|[$pipelineId]|$source| start fail $e", e)
            } finally {
                pipelineBuildLock.unlock()
            }
        }
    }

    fun PipelineBuildStartEvent.execute() {

        LogUtils.addLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId,
            message = "Enter BuildStartControl",
            tag = tag,
            jobId = "",
            executeCount = 1
        )

        val buildInfo = pickUpReadyBuild() ?: return

        val model = buildDetailService.getBuildModel(buildId) ?: run {
            logger.warn("[$pipelineId]|BUILD_START_BAD_MODEL|$source|not exist build detail")
            pipelineEventDispatcher.dispatch(
                PipelineBuildCancelEvent(
                    source = tag, projectId = projectId, pipelineId = pipelineId,
                    userId = userId, buildId = buildId, status = BuildStatus.UNEXEC
                )
            )
            return
        }

        // 单步重试不做操作，手动重试需还原个节点状态，启动需获取revision信息
        LogUtils.addLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId,
            message = "Async fetch latest commit/revision, please wait...",
            tag = tag,
            jobId = "",
            executeCount = 1
        )
        buildModel(this, model)
        LogUtils.addLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId,
            message = "Async fetch latest commit/revision is finish.",
            tag = tag,
            jobId = "",
            executeCount = 1
        )

        if (BuildStatus.isReadyToRun(buildInfo.status)) {

            updateModel(model, pipelineId, buildId, taskId)
            // 这入启动参数
            writeStartParam(projectId, pipelineId, buildId, model)

            val projectName = projectOauthTokenService.getProjectName(projectId) ?: ""
            val pipelineUserInfo = pipelineUserService.get(pipelineId)!!
            val map = mapOf(
                PIPELINE_BUILD_ID to buildId,
                PROJECT_NAME to projectId,
                PROJECT_NAME_CHINESE to projectName,
                PIPELINE_TIME_START to System.currentTimeMillis().toString(),
                PIPELINE_ID to pipelineId,
                PIPELINE_CREATE_USER to pipelineUserInfo.creator,
                PIPELINE_UPDATE_USER to pipelineUserInfo.modifier
            )

            pipelineRuntimeService.batchSetVariable(projectId, pipelineId, buildId, map)
        }
        // 空节点
        if (model.stages.size == 1) {
            logger.warn("[$buildId]|BUILD_START_NO_STAGE|$source| $pipelineId have no stage, go to finish!")
            pipelineEventDispatcher.dispatch(
                PipelineBuildFinishEvent(
                    source = tag,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    buildId = buildId,
                    status = BuildStatus.SUCCEED
                )
            )
            return
        }

        pipelineEventDispatcher.dispatch(
            PipelineBuildStageEvent(
                source = tag,
                projectId = projectId, pipelineId = pipelineId, userId = userId, buildId = buildId,
                stageId = model.stages[1].id ?: modelStageIdGenerator.getNextId(),
                actionType = actionType
            )
        )

        LogUtils.addLine(
            rabbitTemplate = rabbitTemplate,
            buildId = buildId,
            message = "BuildStartControl End",
            tag = tag,
            jobId = "",
            executeCount = 1
        )
    }

    private fun PipelineBuildStartEvent.pickUpReadyBuild(): BuildInfo? {

        val buildInfo: BuildInfo?
        val buildIdLock = BuildIdLock(redisOperation, buildId)
        try {

            buildIdLock.lock()
            buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            if (buildInfo == null || BuildStatus.isFinish(buildInfo.status)) {
                logger.info("[$buildId]|BUILD_START_HAD_DONE|$source|status=${buildInfo?.status}")
                return null
            }

            logger.info("[$buildId]|[${buildInfo.status}]|BUILD_START|$source")
            if (BuildStatus.isReadyToRun(buildInfo.status)) {
                val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(pipelineId)
                if (buildSummaryRecord!!.runningCount > 0) {
                    val setting = pipelineRepositoryService.getSetting(pipelineId)
                    val response = runLockInterceptor.checkRunLock(setting!!.runLockType, pipelineId)
                    if (response.data != BuildStatus.RUNNING) {
                        if (buildInfo.status == BuildStatus.QUEUE_CACHE) { // 重新入队
                            pipelineRuntimeService.updateBuildInfoStatus2Queue(buildId, buildInfo.status)
                        }
                        logger.info("[$buildId]|[${buildInfo.status}]|BUILD_IN_QUEUE|$source|response=$response")
                        return null
                    }
                }

                pipelineRuntimeService.startLatestRunningBuild(
                    latestRunningBuild = LatestRunningBuild(
                        pipelineId = pipelineId,
                        buildId = buildId,
                        userId = buildInfo.startUser,
                        status = BuildStatus.RUNNING,
                        taskCount = buildInfo.taskCount,
                        buildNum = buildInfo.buildNum
                    ),
                    retry = (actionType == ActionType.RETRY)
                )
                pipelineEventDispatcher.dispatch(
                    PipelineBuildStartBroadCastEvent(
                        source = tag,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        userId = buildInfo.startUser,
                        buildId = buildId,
                        startTime = buildInfo.startTime,
                        triggerType = buildInfo.trigger
                    )
                )
            }
        } finally {
            buildIdLock.unlock()
        }
        return buildInfo
    }

    private fun writeStartParam(projectId: String, pipelineId: String, buildId: String, model: Model) {
        val triggerContainer = model.stages[0].containers[0] as TriggerContainer

        if (triggerContainer.buildNo != null) {
            val buildNo = pipelineRuntimeService.getBuildNo(pipelineId)
            pipelineRuntimeService.setVariable(
                projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, varName = BUILD_NO, varValue = buildNo
            )
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

    private fun updateModel(model: Model, pipelineId: String, buildId: String, taskId: String) {
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

        if (!find) {
            logger.warn("[$buildId]|[$pipelineId]| Fail to find the startTask $taskId")
        } else {
            container.status = BuildStatus.SUCCEED.name
            container.systemElapsed = 0
            container.elementElapsed = 0

            buildDetailService.updateModel(buildId, model)
        }
    }

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
                        if (BuildStatus.isFinish(eleStatus)) {
                            callScm = false
                            ele.status = ""
                            ele.elapsed = null
                            logger.info("[$pipelineId-${ele.id}] is retry,clean status and keep revision")
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

                    logger.info("[$pipelineId]| fetchLatestRevision($repositoryConfig, $branchName)")

                    if (callScm) {
                        logger.info("[$pipelineId-${ele.id}] is start,get revision by scmService")
                        val latestRevision =
                            scmService.recursiveFetchLatestRevision(
                                projectId,
                                pipelineId,
                                repositoryConfig,
                                branchName,
                                variables
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
                        } else {
                            logger.warn("[$pipelineId] get git latestRevision empty! msg=${latestRevision.message}")
                        }
                    }
                }
            }
        }
    }

    private fun buildModel(event: PipelineBuildStartEvent, sModel: Model) {
        if (event.actionType == ActionType.RETRY) {
            return
        }
        if (event.actionType == ActionType.START) {
            val startParams = pipelineRuntimeService.getAllVariable(event.buildId)
            if (startParams.isNotEmpty()) {
                supplementModel(event.projectId, event.pipelineId, sModel, startParams as MutableMap<String, String>)
            } else {
                val variables = mutableMapOf<String, String>()
                supplementModel(event.projectId, event.pipelineId, sModel, variables)
            }
        }
    }
}
