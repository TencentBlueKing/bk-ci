package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NotifySendCmd @Autowired constructor(
    val client: Client
): NotifyCmd {
    override fun canExecute(commandContextBuild: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContextBuild: BuildNotifyContext) {
        val buildStatus = commandContextBuild.buildStatus
        when {
            buildStatus.isFailure() -> {
                sendNotifyByTemplate(
                    templateCode = PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE,
                    receivers = commandContextBuild.receivers,
                    notifyType = commandContextBuild.pipelineSetting.failSubscription.types.map { it.name }.toMutableSet(),
                    titleParams = commandContextBuild.notifyValue,
                    bodyParams = commandContextBuild.notifyValue
                )
            }
            buildStatus.isCancel() -> {
                sendNotifyByTemplate(
                    templateCode = PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE,
                    receivers = commandContextBuild.receivers,
                    notifyType = commandContextBuild.pipelineSetting.failSubscription.types.map { it.name }.toMutableSet(),
                    titleParams = commandContextBuild.notifyValue,
                    bodyParams = commandContextBuild.notifyValue
                )
            }
            buildStatus.isSuccess() -> {
                sendNotifyByTemplate(
                    templateCode = PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE,
                    receivers = commandContextBuild.receivers,
                    notifyType = commandContextBuild.pipelineSetting.successSubscription.types.map { it.name }.toMutableSet(),
                    titleParams = commandContextBuild.notifyValue,
                    bodyParams = commandContextBuild.notifyValue
                )
            }
            else -> Result<Any>(0)
        }
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
}
