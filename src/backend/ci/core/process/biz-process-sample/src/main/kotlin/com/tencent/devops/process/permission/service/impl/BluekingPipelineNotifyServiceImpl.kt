package com.tencent.devops.process.permission.service.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.service.PipelineNotifyService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_VERSION
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnMissingBean(PipelineNotifyService::class)
class BluekingPipelineNotifyServiceImpl @Autowired constructor(
    override val buildVariableService: BuildVariableService,
    override val pipelineRuntimeService: PipelineRuntimeService,
    override val pipelineRepositoryService: PipelineRepositoryService,
    override val pipelineSettingDao: PipelineSettingDao,
    override val dslContext: DSLContext,
    override val client: Client,
    override val pipelineBuildFacadeService: PipelineBuildFacadeService
) : PipelineNotifyService(
    buildVariableService,
    pipelineRuntimeService,
    pipelineRepositoryService,
    pipelineSettingDao,
    dslContext,
    client,
    pipelineBuildFacadeService
) {
    override fun getExecutionVariables(pipelineId: String, vars: MutableMap<String, String>): ExecutionVariables {
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

    override fun sendWeworkGroupMsg(setting: PipelineSetting, buildStatus: BuildStatus, vars: MutableMap<String, String>) {
        return
    }

    override fun buildUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vars: MutableMap<String, String>
    ): Map<String, String> {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(pipelineId) ?: return emptyMap()
        var pipelineName = pipelineInfo.pipelineName
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return emptyMap()

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
        return mutableMapOf(
            "detailUrl" to detailUrl,
            "detailOuterUrl" to detailUrl,
            "detailShortOuterUrl" to detailUrl
        )
    }

    override fun getReceivers(
        setting: PipelineSetting,
        type: String,
        projectId: String,
        vars: MutableMap<String, String>
    ): Set<String> {
        return if (type == SUCCESS_TYPE) {
            setting.successSubscription.users.split(",").toMutableSet()
        } else {
            setting.failSubscription.users.split(",").toMutableSet()
        }
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
}
