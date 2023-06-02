package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NotifySendCmd @Autowired constructor(
    val client: Client
) : NotifyCmd {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val buildStatus = commandContext.buildStatus
        val setting = commandContext.pipelineSetting
        val shutdownType = when {
            buildStatus.isCancel() -> TYPE_SHUTDOWN_CANCEL
            buildStatus.isFailure() -> TYPE_SHUTDOWN_FAILURE
            else -> TYPE_SHUTDOWN_SUCCESS
        }

        var templateCode = ""
        var notifyType = mutableSetOf<String>()
        val settingDetailFlag: Boolean
        var sendMsg = false

        when {
            buildStatus.isFailure() -> {
                settingDetailFlag = setting.failSubscription.detailFlag
                templateCode = getNotifyTemplateCode(shutdownType, settingDetailFlag)
                notifyType = setting.failSubscription.types.map { it.name }.toMutableSet()
                sendMsg = true
            }
            buildStatus.isSuccess() -> {
                settingDetailFlag = setting.successSubscription.detailFlag
                templateCode = getNotifyTemplateCode(shutdownType, settingDetailFlag)
                notifyType = setting.successSubscription.types.map { it.name }.toMutableSet()
                sendMsg = true
            }
            else -> Result<Any>(0)
        }

        if (sendMsg) {
            sendNotifyByTemplate(
                templateCode = templateCode,
                receivers = commandContext.receivers,
                notifyType = notifyType,
                titleParams = commandContext.notifyValue,
                bodyParams = commandContext.notifyValue
            )
        }
    }

    private fun getNotifyTemplateCode(type: Int, detailFlag: Boolean): String {
        return if (detailFlag) {
            when (type) {
                TYPE_STARTUP ->
                    PipelineNotifyTemplateEnum.PIPELINE_STARTUP_NOTIFY_TEMPLATE_DETAIL.templateCode
                TYPE_SHUTDOWN_SUCCESS ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE_DETAIL.templateCode
                TYPE_SHUTDOWN_FAILURE ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE_DETAIL.templateCode
                TYPE_SHUTDOWN_CANCEL ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE_DETAIL.templateCode
                else ->
                    throw IllegalArgumentException("Unknown type($type) of Notify")
            }
        } else {
            when (type) {
                TYPE_STARTUP -> PipelineNotifyTemplateEnum.PIPELINE_STARTUP_NOTIFY_TEMPLATE.templateCode
                TYPE_SHUTDOWN_SUCCESS ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE.templateCode
                TYPE_SHUTDOWN_FAILURE ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE.templateCode
                TYPE_SHUTDOWN_CANCEL ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE.templateCode
                else ->
                    throw IllegalArgumentException("Unknown type($type) of Notify")
            }
        }
    }

    private fun sendNotifyByTemplate(
        templateCode: String,
        receivers: Set<String>,
        notifyType: Set<String>,
        titleParams: Map<String, String>,
        bodyParams: Map<String, String>
    ) {
        client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
            SendNotifyMessageTemplateRequest(
                templateCode = templateCode,
                receivers = receivers as MutableSet<String>,
                notifyType = notifyType as MutableSet<String>,
                titleParams = titleParams,
                bodyParams = bodyParams,
                cc = null,
                bcc = null
            )
        )
    }

    companion object {
        const val TYPE_STARTUP = 1
        const val TYPE_SHUTDOWN_SUCCESS = 2
        const val TYPE_SHUTDOWN_FAILURE = 3
        const val TYPE_SHUTDOWN_CANCEL = 4
    }
}
