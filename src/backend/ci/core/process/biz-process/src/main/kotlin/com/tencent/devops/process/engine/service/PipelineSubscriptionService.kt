package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.process.utils.PIPELINE_VERSION
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@Service
class PipelineSubscriptionService constructor(
    private val dslContext: DSLContext,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client,
    private val buildVariableService: BuildVariableService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
) {

    fun onPipelineShutdown(
        pipelineId: String,
        buildId: String,
        projectId: String,
        buildStatus: BuildStatus
    ) {
        logger.info("onPipelineShutdown $pipelineId|$buildId|$buildStatus")
        val vars = buildVariableService.getAllVariable(projectId, buildId).toMutableMap()
        vars[PIPELINE_TIME_DURATION]?.takeIf { it.isNotBlank() }?.toLongOrNull()?.let {
            vars[PIPELINE_TIME_DURATION] = DateTimeUtil.formatMillSecond(it * 1000)
        }
        val executionVar = getExecutionVariables(pipelineId, vars)
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId) ?: return
        logger.info("buildInfo is $buildInfo")
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        var pipelineName = pipelineInfo.pipelineName

        // 判断codecc类型更改查看详情链接
        val detailUrl = if (pipelineInfo.channelCode == ChannelCode.CODECC) {
            val detail = pipelineBuildFacadeService.getBuildDetail(userId = buildInfo.startUser,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = ChannelCode.BS,
                checkPermission = false)
            val codeccModel = getCodeccTaskName(detail)
            if (codeccModel != null) {
                pipelineName = codeccModel.codeCCTaskName.toString()
            }
            val taskId = pipelineName
            "${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/task/$taskId/detail"
        } else {
            detailUrl(projectId, pipelineId, buildId)
        }

        val trigger = executionVar.trigger
        val buildNum = buildInfo.buildNum
        val user = executionVar.user

        logger.info("onPipelineShutdown pipelineNameReal:$pipelineName")
        val replaceWithEmpty = true
        // 流水线设置订阅的用户
        val setting = pipelineSettingDao.getSetting(dslContext, projectId, pipelineId) ?: return
        setting.successReceiver = EnvUtils.parseEnv(setting.successReceiver, vars, replaceWithEmpty)
        setting.failReceiver = EnvUtils.parseEnv(setting.failReceiver, vars, replaceWithEmpty)
        // 内容为null的时候处理为空字符串
        setting.successContent = setting.successContent ?: NotifyTemplateUtils.COMMON_SHUTDOWN_SUCCESS_CONTENT
        setting.failContent = setting.failContent ?: NotifyTemplateUtils.COMMON_SHUTDOWN_FAILURE_CONTENT
        // 内容
        var emailSuccessContent = setting.successContent
        var emailFailContent = setting.failContent
        val detail = pipelineBuildFacadeService.getBuildDetail(buildInfo.startUser,
            projectId,
            pipelineId,
            buildId,
            ChannelCode.BS,
            false)
        val failTask = getFailTaskName(detail)
        vars["failTask"] = failTask
        emailSuccessContent = EnvUtils.parseEnv(emailSuccessContent, vars, replaceWithEmpty)
        emailFailContent = EnvUtils.parseEnv(emailFailContent, vars, replaceWithEmpty)
        setting.successContent = EnvUtils.parseEnv(setting.successContent, vars, replaceWithEmpty)

        setting.failContent = EnvUtils.parseEnv(setting.failContent, vars, replaceWithEmpty)

        val projectName =
            client.get(ServiceProjectResource::class).get(projectId).data?.projectName.toString()
        val mapData = mutableMapOf(
            "pipelineName" to pipelineName,
            "buildNum" to buildNum.toString(),
            "projectName" to projectName,
            "detailUrl" to detailUrl,
            "detailOuterUrl" to detailUrl,
            "detailShortOuterUrl" to detailUrl,
            "startTime" to getFormatTime(detail.startTime),
            "trigger" to trigger,
            "username" to user,
            "detailUrl" to detailUrl,
            "successContent" to setting.successContent,
            "failContent" to setting.failContent,
            "emailSuccessContent" to emailSuccessContent,
            "emailFailContent" to emailFailContent,
            "failTask" to failTask
        )
        // 把流水线变量带上
        val params = vars + mapData
        val result = when {
            buildStatus.isFailure() -> {
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
                    SendNotifyMessageTemplateRequest(
                        templateCode = PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE.templateCode,
                        receivers = setting.failReceiver.split(",").toMutableSet(),
                        notifyType = setting.failType.split(",").toMutableSet(),
                        titleParams = params,
                        bodyParams = params,
                        cc = null,
                        bcc = null
                    )
                )
            }
            buildStatus.isCancel() -> {
                // 取消暂时按失败的配置
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
                    SendNotifyMessageTemplateRequest(
                        templateCode = PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE.templateCode,
                        receivers = setting.failReceiver.split(",").toMutableSet(),
                        notifyType = setting.failType.split(",").toMutableSet(),
                        titleParams = params,
                        bodyParams = params,
                        cc = null,
                        bcc = null
                    )
                )
            }
            buildStatus.isSuccess() -> {
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
                    SendNotifyMessageTemplateRequest(
                        templateCode = PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE.templateCode,
                        receivers = setting.successReceiver.split(",").toMutableSet(),
                        notifyType = setting.successType.split(",").toMutableSet(),
                        titleParams = mapData,
                        bodyParams = mapData,
                        cc = null,
                        bcc = null
                    )
                )
            }
            else -> Result<Any>(0)
        }
        if (result.isNotOk()) {
            logger.warn("onPipelineShutdown notify failed: $result")
        }
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

    private fun getCodeccTaskName(detail: ModelDetail): LinuxCodeCCScriptElement? {
        for (stage in detail.model.stages) {
            stage.containers.forEach { container ->
                val codeccElemet =
                    container.elements.filter { it is LinuxCodeCCScriptElement || it is LinuxPaasCodeCCScriptElement }
                if (codeccElemet.isNotEmpty()) return codeccElemet.first() as LinuxCodeCCScriptElement
            }
        }
        return null
    }

    private fun detailUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${HomeHostUtil.outerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$processInstanceId"

    fun getExecutionVariables(pipelineId: String, vars: Map<String, String>): ExecutionVariables {
        var buildUser = ""
        var triggerType = ""
        var buildNum: Int? = null
        var pipelineVersion: Int? = null
        var channelCode: ChannelCode? = null
        var webhookTriggerUser: String? = null
        var pipelineUserId: String? = null
        var isMobileStart = false

        vars.forEach { (key, value) ->
            when (key) {
                PIPELINE_VERSION -> pipelineVersion = value.toInt()
                PIPELINE_START_USER_ID -> buildUser = value
                PIPELINE_START_TYPE -> triggerType = value
                PIPELINE_BUILD_NUM -> buildNum = value.toInt()
                PIPELINE_START_CHANNEL -> channelCode = ChannelCode.valueOf(value)
                PIPELINE_START_WEBHOOK_USER_ID -> webhookTriggerUser = value
                PIPELINE_START_PIPELINE_USER_ID -> pipelineUserId = value
                PIPELINE_START_MOBILE -> isMobileStart = value.toBoolean()
            }
        }

        // 对于是web hook 触发的构建，用户显示触发人
        when (triggerType) {
            StartType.WEB_HOOK.name -> {
                webhookTriggerUser?.takeIf { it.isNotBlank() }?.let {
                    buildUser = it
                }
            }
            StartType.PIPELINE.name -> {
                pipelineUserId?.takeIf { it.isNotBlank() }?.let {
                    buildUser = it
                }
            }
        }

        val trigger = StartType.toReadableString(triggerType, channelCode)
        return ExecutionVariables(pipelineVersion = pipelineVersion,
            buildNum = buildNum,
            trigger = trigger,
            originTriggerType = triggerType,
            user = buildUser,
            isMobileStart = isMobileStart)
    }

    private fun getFormatTime(time: Long): String {
        val current = LocalDateTime.ofInstant(Date(time).toInstant(), ZoneId.systemDefault())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineSubscriptionService::class.java)
    }

    data class ExecutionVariables(
        val pipelineVersion: Int?,
        val buildNum: Int?,
        val trigger: String,
        val originTriggerType: String,
        val user: String,
        val isMobileStart: Boolean
    )
}
