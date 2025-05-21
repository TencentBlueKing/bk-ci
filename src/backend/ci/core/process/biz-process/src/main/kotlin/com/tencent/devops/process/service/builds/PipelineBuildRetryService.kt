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

package com.tencent.devops.process.service.builds

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.StageIdLock
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.WebhookBuildParameterService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineTaskPauseService
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.utils.PIPELINE_RETRY_ALL_FAILED_CONTAINER
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_RETRY_RUNNING_BUILD
import com.tencent.devops.process.utils.PIPELINE_RETRY_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_TASK_IN_CONTAINER_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_TASK_IN_STAGE_ID
import com.tencent.devops.process.utils.PIPELINE_SKIP_FAILED_TASK
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineBuildRetryService @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val redisOperation: RedisOperation,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildDetailService: PipelineBuildDetailService,
    private val webhookBuildParameterService: WebhookBuildParameterService,
    private val buildVariableService: BuildVariableService,
    private val pipelineTaskPauseService: PipelineTaskPauseService,
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineContainerService: PipelineContainerService
) {

    @Value("\${pipeline.build.retry.limit_days:28}")
    private var retryLimitDays: Int = 28

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    fun retry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String, // 要重试的构建ID
        taskId: String? = null, // 要重试或跳过的插件ID，或者StageId, 或则stepId
        failedContainer: Boolean? = false, // 仅重试所有失败Job
        skipFailedTask: Boolean? = false, // 跳过失败插件，为true时需要传taskId值（值为stageId则表示跳过Stage下所有失败插件）
        isMobile: Boolean = false,
        channelCode: ChannelCode? = ChannelCode.BS,
        checkPermission: Boolean? = true
    ): BuildId {
        if (checkPermission!!) {
            val permission = AuthPermission.EXECUTE
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), pipelineId)
                )
            )
        }

        BuildIdLock(redisOperation = redisOperation, buildId = buildId).use { redisLock ->
            redisLock.lock()

            val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId)
                )

            if (buildInfo.pipelineId != pipelineId) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT)
            }
            buildInfo.startTime?.let {
                // 判断当前时间是否超过最大重试时间
                if (LocalDateTime.now().minusDays(retryLimitDays.toLong()).timestampmilli() - it > 0) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_RETRY_TIME_INVALID,
                        params = arrayOf(retryLimitDays.toString())
                    )
                }
            }

            val (readyToBuildPipelineInfo, resource, _) = pipelineRepositoryService.getBuildTriggerInfo(
                projectId, pipelineId, buildInfo.version
            )
            if (readyToBuildPipelineInfo.locked == true) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
            }

            // TODO 重试的model需要被覆盖为上次构建的内容，未来需要替换为RECORD表数据
            resource.model = buildDetailService.getBuildModel(projectId, buildId) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )

            val paramMap = mutableMapOf<String, BuildParameters>()
            // #2821 构建重试均要传递触发插件ID，否则当有多个事件触发插件时，rebuild后触发器的标记不对
            buildVariableService.getVariable(
                projectId, pipelineId, buildId, PIPELINE_START_TASK_ID
            )?.let { startTaskId ->
                paramMap[PIPELINE_START_TASK_ID] = BuildParameters(PIPELINE_START_TASK_ID, startTaskId)
            }

            val webHookStartParam = mutableMapOf<String, BuildParameters>()
            buildInfo.buildParameters?.forEach { param -> webHookStartParam[param.key] = param }
            webhookBuildParameterService.getBuildParameters(buildId)?.forEach { param ->
                webHookStartParam[param.key] = param
            }

            if (!taskId.isNullOrBlank()) {
                findRetryStartTaskId(
                    userId = userId,
                    projectId = projectId,
                    buildId = buildId,
                    taskId = taskId,
                    skipFailedTask = skipFailedTask,
                    failedContainer = failedContainer,
                    buildInfo = buildInfo,
                    paramMap = paramMap,
                    resource = resource
                )
            } else {
                // 完整构建重试，去掉启动参数中的重试插件ID保证不冲突，同时保留重试次数，并清理VAR表内容
                try {
                    paramMap.putAll(webHookStartParam)
                    val setting = pipelineRepositoryService.getSetting(projectId, pipelineId)
                    if (setting?.cleanVariablesWhenRetry == true) {
                        buildVariableService.deleteBuildVars(projectId, pipelineId, buildId)
                    }
                } catch (ignored: Exception) {
                    logger.warn("ENGINE|$buildId|Fail to get the startup param: $ignored")
                }
            }

            // 重置因暂停而变化的element(需同时支持流水线重试和stage重试, task重试), model不在这保存，在startBuild中保存
            pipelineTaskPauseService.resetElementWhenPauseRetry(projectId, buildId, resource.model)

            // rebuild重试计数
            val retryCount = (paramMap[PIPELINE_RETRY_COUNT]?.value?.toString()?.toInt() ?: 0) + 1

            logger.info(
                "ENGINE|$buildId|RETRY_PIPELINE_ORIGIN|taskId=$taskId|$pipelineId|" +
                        "retryCount=$retryCount|fc=$failedContainer|skip=$skipFailedTask"
            )

            paramMap[PIPELINE_RETRY_COUNT] = BuildParameters(PIPELINE_RETRY_COUNT, retryCount)
            paramMap[PIPELINE_RETRY_BUILD_ID] = BuildParameters(PIPELINE_RETRY_BUILD_ID, buildId, readOnly = true)

            val startType = StartType.toStartType(buildInfo.trigger)
            return if (!buildInfo.isFinish()) {
                runningBuildRetry(
                    userId = userId,
                    pipeline = readyToBuildPipelineInfo,
                    startType = startType,
                    paramMap = paramMap,
                    channelCode = channelCode ?: ChannelCode.BS,
                    isMobile = isMobile,
                    resource = resource,
                    buildInfo = buildInfo,
                    webHookStartParam = webHookStartParam
                )
            } else {
                pipelineBuildService.startPipeline(
                    userId = userId,
                    pipeline = readyToBuildPipelineInfo,
                    startType = startType,
                    pipelineParamMap = paramMap,
                    channelCode = channelCode ?: ChannelCode.BS,
                    isMobile = isMobile,
                    resource = resource,
                    signPipelineVersion = buildInfo.version,
                    frequencyLimit = true,
                    handlePostFlag = false,
                    debug = buildInfo.debug,
                    webHookStartParam = webHookStartParam
                )
            }
        }
    }

    private fun runningBuildRetry(
        userId: String,
        pipeline: PipelineInfo,
        startType: StartType,
        paramMap: MutableMap<String, BuildParameters>,
        channelCode: ChannelCode,
        isMobile: Boolean,
        resource: PipelineResourceVersion,
        buildInfo: BuildInfo,
        webHookStartParam: MutableMap<String, BuildParameters> = mutableMapOf()
    ): BuildId {
        if (buildInfo.isStageSuccess()) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_DUPLICATE_BUILD_RETRY_ACT)
        }
        val retryTaskStageId = paramMap[PIPELINE_RETRY_TASK_IN_STAGE_ID]?.value?.toString()
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_DUPLICATE_BUILD_RETRY_ACT)
        // 运行中重试,需要判断当前的stage的状态,防止当前stage已经执行完成,准备往下一个stage执行,那么运行中重试就会导致执行异常
        val stageIdLock = StageIdLock(redisOperation, buildInfo.buildId, retryTaskStageId)
        stageIdLock.use {
            if (!stageIdLock.tryLock()) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_DUPLICATE_BUILD_RETRY_ACT)
            }
            val stage = pipelineStageService.getStage(pipeline.projectId, buildInfo.buildId, retryTaskStageId) ?: run {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_BUILD_EXPIRED_CANT_RETRY
                )
            }
            // 只有stage是running状态才运行插件级重试
            if (stage.status.isFinish()) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_RETRY_STAGE_NOT_RUNNING
                )
            }

            return pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = pipeline,
                startType = startType,
                pipelineParamMap = paramMap,
                channelCode = channelCode,
                isMobile = isMobile,
                resource = resource,
                signPipelineVersion = buildInfo.version,
                frequencyLimit = true,
                handlePostFlag = false,
                debug = buildInfo.debug,
                webHookStartParam = webHookStartParam
            )
        }
    }

    private fun findRetryStartTaskId(
        userId: String,
        projectId: String,
        buildId: String,
        taskId: String?,
        failedContainer: Boolean?,
        skipFailedTask: Boolean?,
        buildInfo: BuildInfo,
        paramMap: MutableMap<String, BuildParameters>,
        resource: PipelineResourceVersion
    ) {
        buildInfo.buildParameters?.associateBy { it.key }?.get(PIPELINE_RETRY_COUNT)?.let { param ->
            paramMap[param.key] = param
        }
        // stage/job/task级重试
        resource.model.stages.forEach { s ->
            // stage 级重试
            if (s.id == taskId) {
                fillRetryStageParams(projectId, buildId, s, paramMap, failedContainer, skipFailedTask)
                return
            }
            s.containers.forEach { c ->
                val pos = if (c.id == taskId) 0 else -1 // 容器job级别的重试，则找job的第一个原子
                c.elements.forEachIndexed { index, element ->
                    if (index == pos) {
                        pipelineContainerService.getContainer(projectId, buildId, s.id, c.id!!) ?: run {
                            throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_BUILD_EXPIRED_CANT_RETRY
                            )
                        }
                        paramMap[PIPELINE_RETRY_START_TASK_ID] = BuildParameters(
                            key = PIPELINE_RETRY_START_TASK_ID, value = element.id!!
                        )
                        return
                    }
                    if (element.id == taskId || element.stepId == taskId) {
                        fillRetryTaskParams(
                            userId = userId,
                            projectId = projectId,
                            buildId = buildId,
                            buildInfo = buildInfo,
                            stage = s,
                            container = c,
                            element = element,
                            taskId = taskId,
                            paramMap = paramMap,
                            skipFailedTask = skipFailedTask,
                        )
                        return
                    }
                }
            }
        }
    }

    /**
     * 填充stage级重试参数
     */
    private fun fillRetryStageParams(
        projectId: String,
        buildId: String,
        stage: Stage,
        paramMap: MutableMap<String, BuildParameters>,
        failedContainer: Boolean?,
        skipFailedTask: Boolean?
    ) {
        val buildStage = pipelineStageService.getStage(projectId, buildId, stageId = stage.id) ?: run {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_BUILD_EXPIRED_CANT_RETRY
            )
        }
        // 只有失败或取消情况下提供重试得可能
        if (!buildStage.status.isFailure() && !buildStage.status.isCancel()) throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_RETRY_STAGE_NOT_FAILED
        )
        paramMap[PIPELINE_RETRY_START_TASK_ID] = BuildParameters(
            key = PIPELINE_RETRY_START_TASK_ID, value = stage.id!!
        )
        paramMap[PIPELINE_RETRY_ALL_FAILED_CONTAINER] = BuildParameters(
            key = PIPELINE_RETRY_ALL_FAILED_CONTAINER,
            value = failedContainer ?: false,
            valueType = BuildFormPropertyType.TEMPORARY
        )
        paramMap[PIPELINE_SKIP_FAILED_TASK] = BuildParameters(
            key = PIPELINE_SKIP_FAILED_TASK,
            value = skipFailedTask ?: false,
            valueType = BuildFormPropertyType.TEMPORARY
        )
    }

    /**
     * 填充task级重试参数
     */
    private fun fillRetryTaskParams(
        userId: String,
        projectId: String,
        buildId: String,
        buildInfo: BuildInfo,
        stage: Stage,
        container: Container,
        element: Element,
        taskId: String?,
        paramMap: MutableMap<String, BuildParameters>,
        skipFailedTask: Boolean?,
    ) {
        // 校验task是否允许跳过
        if (skipFailedTask == true) {
            val isSkipTask = isSkipTask(
                userId = userId,
                projectId = projectId,
                manualSkip = element.additionalOptions?.manualSkip
            )
            if (!isSkipTask) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TASK_NOT_ALLOWED_TO_BE_SKIPPED
                )
            }
        }
        pipelineTaskService.getByTaskId(
            transactionContext = null,
            projectId = projectId,
            buildId = buildId,
            taskId = element.id
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_BUILD_EXPIRED_CANT_RETRY
        )

        paramMap[PIPELINE_RETRY_START_TASK_ID] = BuildParameters(
            key = PIPELINE_RETRY_START_TASK_ID, value = element.id!!
        )
        paramMap[PIPELINE_SKIP_FAILED_TASK] = BuildParameters(
            key = PIPELINE_SKIP_FAILED_TASK,
            value = skipFailedTask ?: false,
            valueType = BuildFormPropertyType.TEMPORARY
        )
        // 重试运行中的构建
        if (!buildInfo.isFinish()) {
            paramMap[PIPELINE_RETRY_RUNNING_BUILD] = BuildParameters(
                key = PIPELINE_RETRY_RUNNING_BUILD,
                value = true,
                valueType = BuildFormPropertyType.TEMPORARY
            )
            paramMap[PIPELINE_RETRY_TASK_IN_STAGE_ID] = BuildParameters(
                key = PIPELINE_RETRY_TASK_IN_STAGE_ID,
                value = stage.id!!,
                valueType = BuildFormPropertyType.TEMPORARY
            )
            paramMap[PIPELINE_RETRY_TASK_IN_CONTAINER_ID] = BuildParameters(
                key = PIPELINE_RETRY_TASK_IN_CONTAINER_ID,
                value = container.id!!,
                valueType = BuildFormPropertyType.TEMPORARY
            )
        }
    }

    private  fun isSkipTask(
        userId: String,
        projectId: String,
        manualSkip: Boolean?
    ): Boolean {
        // 若该task具有手动跳过配置或者用户拥有API特殊操作权限，才允许跳过.
        return manualSkip == true || try {
            pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                authResourceType = AuthResourceType.PROJECT,
                permission = AuthPermission.API_OPERATE
            )
        } catch (ignore: Exception) {
            logger.info("the project has not registered api operation permission:$userId|$projectId|$ignore")
            false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildRetryService::class.java)
    }
}
