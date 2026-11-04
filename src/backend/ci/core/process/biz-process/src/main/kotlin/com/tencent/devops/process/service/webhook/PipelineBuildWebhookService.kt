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

package com.tencent.devops.process.service.webhook

import com.tencent.devops.common.api.context.ChannelContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.pojo.measure.ProjectUserDailyEvent
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsData
import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsEvent
import com.tencent.devops.common.event.pojo.measure.UserOperateCounterData
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineWebHookQueueService
import com.tencent.devops.process.engine.service.WebhookBuildParameterService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.code.WebhookCommit
import com.tencent.devops.process.pojo.webhook.WebhookStartPipelineRequest
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PipelineVarUtil
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Suppress("ALL")
@Service
class PipelineBuildWebhookService @Autowired constructor(
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineWebHookQueueService: PipelineWebHookQueueService,
    private val buildLogPrinter: BuildLogPrinter,
    private val webhookBuildParameterService: WebhookBuildParameterService,
    private val measureEventDispatcher: SampleEventDispatcher,
    private val pipelinePermissionService: PipelinePermissionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildWebhookService::class.java)
        private const val WEBHOOK_COMMIT_TRIGGER = "webhook_commit_trigger"
    }

    /**
     * webhookCommitTriggerPipelineBuild 方法是webhook事件触发最后执行方法
     * @link PipelineBuildWebhookTriggerService 匹配成功后按项目所在集群调用该接口，从而区分正式和灰度服务
     * @param projectId 项目ID
     * @param webhookCommit webhook事件信息
     *
     */
    fun webhookCommitTriggerPipelineBuild(projectId: String, webhookCommit: WebhookCommit): BuildId? {
        val userId = webhookCommit.userId
        val pipelineId = webhookCommit.pipelineId
        val startParams = webhookCommit.params

        val repoName = webhookCommit.repoName
        // 如果是分支版本的触发，此处一定要传指定的version号，按指定版本触发
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
        // webhook由外部事件经MQ触发,当前线程无渠道上下文,以流水线自身渠道恢复,
        // 确保创作流等渠道的权限校验能命中正确的权限中心资源类型
        ChannelContext.withChannel(pipelineInfo.channelCode.name) {
            checkPermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            )
        }
        if (pipelineInfo.locked == true) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
        }
        // 代码库触发支持仅有分支版本的情况，如果仅有草稿需要在这里拦截
        if (pipelineInfo.latestVersionStatus == VersionStatus.COMMITTING) throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_RELEASE_PIPELINE_VERSION
        )
        val version = webhookCommit.version ?: pipelineInfo.version

        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
        if (resource == null) {
            logger.warn("[$pipelineId]| Fail to get the model")
            return null
        }

        // 兼容从旧v1版本下发过来的请求携带旧的变量命名
        val pipelineParamMap = convertBuildParameters(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            triggerContainer = resource.model.getTriggerContainer(),
            startParams = startParams
        )

        val startEpoch = System.currentTimeMillis()
        val channelCode = pipelineInfo.channelCode
        try {
            val buildId = pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = pipelineInfo,
                startType = StartType.WEB_HOOK,
                pipelineParamMap = HashMap(pipelineParamMap),
                channelCode = channelCode,
                isMobile = false,
                resource = resource,
                signPipelineVersion = version,
                frequencyLimit = false
            )
            pipelineWebHookQueueService.onWebHookTrigger(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId.id,
                variables = webhookCommit.params,
                channelCode = channelCode
            )
            // #2958 webhook触发在触发原子上输出变量
            buildLogPrinter.addLines(
                buildId = buildId.id,
                logMessages = pipelineParamMap.map {
                    LogMessage(
                        message = "${it.key}=${it.value.value}",
                        timestamp = System.currentTimeMillis(),
                        tag = startParams[PIPELINE_START_TASK_ID]?.toString() ?: ""
                    )
                }
            )
            if (buildId.id.isNotBlank()) {
                webhookBuildParameterService.save(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId.id,
                    buildParameters = pipelineParamMap.values.toList()
                )

                // 上报项目用户度量
                if (startParams[PIPELINE_START_WEBHOOK_USER_ID] != null) {
                    uploadProjectUserMetrics(
                        userId = startParams[PIPELINE_START_WEBHOOK_USER_ID]!!.toString(),
                        projectId = projectId,
                        theDate = LocalDate.now()
                    )
                }
            }
            return buildId
        } catch (ignore: Exception) {
            logger.warn("[$pipelineId]| webhook trigger fail to start repo($repoName): ${ignore.message}", ignore)
            throw ignore
        } finally {
            logger.info("$pipelineId|WEBHOOK_TRIGGER|repo=$repoName|time=${System.currentTimeMillis() - startEpoch}")
        }
    }

    /**
     * SCM 新链路、TAPD、制品、研发商店事件匹配成功后启动流水线
     */
    fun webhookStartPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        request: WebhookStartPipelineRequest
    ): BuildId {
        request.resource?.let { oldResource ->
            return pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = request.pipelineInfo!!,
                startType = request.startType,
                pipelineParamMap = request.pipelineParamMap!!,
                channelCode = request.channelCode!!,
                isMobile = false,
                resource = oldResource,
                signPipelineVersion = oldResource.version,
                frequencyLimit = request.frequencyLimit
            )
        }
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
        val resource = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, request.version)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )
        val pipelineParamMap = convertBuildParameters(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            triggerContainer = resource.model.getTriggerContainer(),
            startParams = request.startParams ?: emptyMap()
        )
        request.pipelineParamMap?.let { pipelineParamMap.putAll(it) }
        return pipelineBuildService.startPipeline(
            userId = userId,
            pipeline = pipelineInfo,
            startType = request.startType,
            pipelineParamMap = pipelineParamMap,
            channelCode = pipelineInfo.channelCode,
            isMobile = false,
            resource = resource,
            signPipelineVersion = resource.version,
            frequencyLimit = request.frequencyLimit
        )
    }

    private fun convertBuildParameters(
        userId: String,
        projectId: String,
        pipelineId: String,
        triggerContainer: TriggerContainer,
        startParams: Map<String, Any>
    ): MutableMap<String, BuildParameters> {
        val pipelineParamMap = mutableMapOf<String, BuildParameters>()
        val paramMap = buildParamCompatibilityTransformer.parseTriggerParam(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            paramProperties = triggerContainer.params,
            paramValues = startParams.mapValues { it.value.toString() }
        )
        pipelineParamMap.putAll(paramMap)
        startParams.forEach {
            if (paramMap.containsKey(it.key)) {
                return@forEach
            }
            // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.key)
            if (newVarName == null) { // 为空表示该变量是新的，或者不需要兼容，直接加入，能会覆盖旧变量转换而来的新变量
                pipelineParamMap[it.key] = BuildParameters(key = it.key, value = it.value)
            } else if (!pipelineParamMap.contains(newVarName)) { // 新变量还不存在，加入
                pipelineParamMap[newVarName] = BuildParameters(key = newVarName, value = it.value)
            }
        }
        return pipelineParamMap
    }

    private fun checkPermission(userId: String, projectId: String, pipelineId: String) {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EXECUTE,
            message = I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT,
                params = arrayOf(userId, projectId, AuthPermission.EXECUTE.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
    }

    private fun uploadProjectUserMetrics(
        userId: String,
        projectId: String,
        theDate: LocalDate
    ) {
        try {
            val projectUserOperateMetricsKey = ProjectUserOperateMetricsData(
                projectId = projectId,
                userId = userId,
                operate = WEBHOOK_COMMIT_TRIGGER,
                theDate = theDate
            ).getProjectUserOperateMetricsKey()
            measureEventDispatcher.dispatch(
                ProjectUserDailyEvent(
                    projectId = projectId,
                    userId = userId,
                    theDate = theDate
                ),
                ProjectUserOperateMetricsEvent(
                    userOperateCounterData = UserOperateCounterData().apply {
                        this.increment(projectUserOperateMetricsKey)
                    }
                )
            )
        } catch (ignored: Exception) {
            logger.error("save auth user metrics", ignored)
        }
    }
}
