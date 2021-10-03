package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

abstract class PipelineNotifyService @Autowired constructor(
    open val buildVariableService: BuildVariableService,
    open val pipelineRuntimeService: PipelineRuntimeService,
    open val pipelineRepositoryService: PipelineRepositoryService,
    open val pipelineSettingDao: PipelineSettingDao,
    open val dslContext: DSLContext,
    open val client: Client,
    open val pipelineBuildFacadeService: PipelineBuildFacadeService
) {
    fun onPipelineShutdown(
        pipelineId: String,
        buildId: String,
        projectId: String,
        buildStatus: BuildStatus
    ) {
        logger.info("onPipelineShutdown new $pipelineId|$buildId|$buildStatus")
        val vars = buildVariableService.getAllVariable(buildId).toMutableMap()
        vars[PIPELINE_TIME_DURATION]?.takeIf { it.isNotBlank() }?.toLongOrNull()?.let {
            vars[PIPELINE_TIME_DURATION] = DateTimeUtil.formatMillSecond(it * 1000)
        }

        val mapData = buildNotifyMapData(projectId, pipelineId, buildId, vars)

        if (mapData.isEmpty()) {
            logger.warn("onPipelineShutdown mapData is empty,$projectId|$pipelineId|$buildId|$buildStatus")
            return
        }

        // 流水线设置订阅的用户
        val settingInfo = pipelineRepositoryService.getSetting(pipelineId) ?: return

        when {
            buildStatus.isFailure() -> {
                sendNotifyByTemplate(
                    templateCode = PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE,
                    receivers = getReceivers(settingInfo, FAIL_TYPE, projectId, vars),
                    notifyType = settingInfo.failSubscription.types.map { it.name }.toMutableSet(),
                    titleParams = mapData,
                    bodyParams = mapData
                )
            }
            buildStatus.isCancel() -> {
                sendNotifyByTemplate(
                    templateCode = PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE,
                    receivers = getReceivers(settingInfo, FAIL_TYPE, projectId, vars),
                    notifyType = settingInfo.failSubscription.types.map { it.name }.toMutableSet(),
                    titleParams = mapData,
                    bodyParams = mapData
                )
            }
            buildStatus.isSuccess() -> {
                sendNotifyByTemplate(
                    templateCode = PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE,
                    receivers = getReceivers(settingInfo, SUCCESS_TYPE, projectId, vars),
                    notifyType = settingInfo.successSubscription.types.map { it.name }.toMutableSet(),
                    titleParams = mapData,
                    bodyParams = mapData
                )
            }
            else -> Result<Any>(0)
        }

        // 发送企业微信群消息
        sendWeworkGroupMsg(settingInfo, buildStatus, mapData as MutableMap<String, String>)
    }

    abstract fun getExecutionVariables(pipelineId: String, vars: MutableMap<String, String>): ExecutionVariables

    abstract fun sendWeworkGroupMsg(setting: PipelineSetting, buildStatus: BuildStatus, vars: MutableMap<String, String>)

    abstract fun buildUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vars: MutableMap<String, String>
    ): Map<String, String>

    abstract fun getReceivers(
        setting: PipelineSetting,
        type: String,
        projectId: String,
        vars: MutableMap<String, String>
    ): Set<String>

    private fun buildPipelineInfo(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vars: MutableMap<String, String>
    ): Map<String, String> {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(pipelineId) ?: return emptyMap()
        var pipelineName = pipelineInfo.pipelineName
        val executionVar = getExecutionVariables(pipelineId, vars)
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return emptyMap()
        val trigger = executionVar.trigger
        val buildNum = buildInfo.buildNum
        val user = executionVar.user
        val detail = pipelineBuildFacadeService.getBuildDetail(buildInfo.startUser,
            projectId,
            pipelineId,
            buildId,
            ChannelCode.BS,
            false)
        val failTask = getFailTaskName(detail)
        vars["failTask"] = failTask
        val projectName =
            client.get(ServiceProjectResource::class).get(projectId).data?.projectName.toString()
        return mutableMapOf(
            "pipelineName" to pipelineName,
            "buildNum" to buildNum.toString(),
            "projectName" to projectName,
            "startTime" to getFormatTime(detail.startTime),
            "trigger" to trigger,
            "username" to user,
            "failTask" to failTask
        )
    }

    private fun sendNotifyByTemplate(
        templateCode: PipelineNotifyTemplateEnum,
        receivers: Set<String>,
        notifyType: Set<String>,
        titleParams: Map<String, String>,
        bodyParams: Map<String, String>
    ) {
        client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
            SendNotifyMessageTemplateRequest(
                templateCode = templateCode.templateCode,
                receivers = receivers as MutableSet<String>,
                notifyType = notifyType as MutableSet<String>,
                titleParams = titleParams,
                bodyParams = bodyParams,
                cc = null,
                bcc = null
            )
        )
    }

    private fun buildNotifyMapData(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vars: MutableMap<String, String>
    ): Map<String, String> {
        val mapData = mutableMapOf<String, String>()
        mapData.putAll(buildUrl(projectId, pipelineId, buildId, vars))
        mapData.putAll(buildNotifyContent(buildId, vars))
        mapData.putAll(buildPipelineInfo(projectId, pipelineId, buildId, vars))

        return mapData
    }

    private fun buildNotifyContent(pipelineId: String, vars: MutableMap<String, String>): Map<String, String> {
        val replaceWithEmpty = true
        val setting = pipelineSettingDao.getSetting(dslContext, pipelineId) ?: return emptyMap()

        // 内容为null的时候处理为空字符串
        var successContent = setting.successContent ?: NotifyTemplateUtils.COMMON_SHUTDOWN_SUCCESS_CONTENT
        var failContent = setting.failContent ?: NotifyTemplateUtils.COMMON_SHUTDOWN_FAILURE_CONTENT

        successContent = EnvUtils.parseEnv(successContent, vars, replaceWithEmpty)
        failContent = EnvUtils.parseEnv(failContent, vars, replaceWithEmpty)

        return mutableMapOf(
            "successContent" to successContent,
            "failContent" to failContent,
            "emailSuccessContent" to successContent,
            "emailFailContent" to failContent
        )
    }

    private fun getFailTaskName(detail: ModelDetail): String {
        var result = "unknown"
        detail.model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.firstOrNull { "FAILED" == it.status }?.let {
                    result = it.name
                }
            }
        }
        return result
    }

    private fun getFormatTime(time: Long): String {
        val current = LocalDateTime.ofInstant(Date(time).toInstant(), ZoneId.systemDefault())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }

    data class ExecutionVariables(
        val pipelineVersion: Int?,
        val buildNum: Int?,
        val trigger: String,
        val originTriggerType: String,
        val user: String,
        val isMobileStart: Boolean
    )

    companion object {
        val logger = LoggerFactory.getLogger(PipelineNotifyService::class.java)
        const val SUCCESS_TYPE = "success"
        const val FAIL_TYPE = "fail"
    }
}
