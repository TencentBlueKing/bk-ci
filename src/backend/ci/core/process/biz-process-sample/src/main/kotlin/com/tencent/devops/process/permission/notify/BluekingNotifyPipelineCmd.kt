package com.tencent.devops.process.permission.notify

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.notify.command.ExecutionVariables
import com.tencent.devops.process.notify.command.impl.NotifyPipelineCmd
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
import org.springframework.beans.factory.annotation.Autowired

class BluekingNotifyPipelineCmd @Autowired constructor(
    override val pipelineRepositoryService: PipelineRepositoryService,
    override val pipelineRuntimeService: PipelineRuntimeService,
    override val pipelineBuildFacadeService: PipelineBuildFacadeService,
    override val client: Client,
    override val buildVariableService: BuildVariableService
) : NotifyPipelineCmd(
    pipelineRepositoryService, pipelineRuntimeService, pipelineBuildFacadeService, client, buildVariableService
) {
    override fun getExecutionVariables(
        pipelineId: String,
        vars: MutableMap<String, String>
    ): ExecutionVariables {
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

        val trigger = StartType.toReadableString(
            triggerType,
            channelCode,
            I18nUtil.getLanguage(I18nUtil.getRequestUserId())
        )
        return ExecutionVariables(pipelineVersion = pipelineVersion,
            buildNum = buildNum,
            trigger = trigger,
            originTriggerType = triggerType,
            user = buildUser,
            isMobileStart = isMobileStart)
    }
}
