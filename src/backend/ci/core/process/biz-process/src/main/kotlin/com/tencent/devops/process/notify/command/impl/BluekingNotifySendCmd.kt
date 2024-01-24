package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.notify.command.BuildNotifyContext
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BluekingNotifySendCmd @Autowired constructor(
    val client: Client
) : NotifySendCmd() {
    override fun canExecute(commandContext: BuildNotifyContext): Boolean {
        return true
    }

    override fun execute(commandContext: BuildNotifyContext) {
        val replaceWithEmpty = true
        val setting = commandContext.pipelineSetting
        val buildStatus = commandContext.buildStatus
        val shutdownType = when {
            buildStatus.isCancel() -> TYPE_SHUTDOWN_CANCEL
            buildStatus.isFailure() -> TYPE_SHUTDOWN_FAILURE
            else -> TYPE_SHUTDOWN_SUCCESS
        }
        when {
            buildStatus.isFailure() -> {
                setting.successSubscriptionList?.forEach { successSubscription ->
                    // 内容为null的时候处理为空字符串
                    val successContent = EnvUtils.parseEnv(
                        successSubscription.content, commandContext.variables, replaceWithEmpty
                    )
                    val params = mapOf(
                        "successContent" to successContent,
                        "emailSuccessContent" to successContent
                    )
                    val receivers = successSubscription.users.split(",").map {
                        EnvUtils.parseEnv(
                            command = it,
                            data = commandContext.variables,
                            replaceWithEmpty = true
                        )
                    }.toSet()
                    sendNotifyByTemplate(
                        templateCode = getNotifyTemplateCode(shutdownType, successSubscription.detailFlag),
                        receivers = receivers,
                        notifyType = successSubscription.types.map { it.name }.toMutableSet(),
                        titleParams = params,
                        bodyParams = params
                    )
                }
            }
            buildStatus.isSuccess() -> {
                setting.failSubscriptionList?.forEach { failSubscription ->
                    // 内容为null的时候处理为空字符串
                    val failContent = EnvUtils.parseEnv(
                        failSubscription.content, commandContext.variables, replaceWithEmpty
                    )
                    val params = mapOf(
                        "failContent" to failContent,
                        "emailFailContent" to failContent
                    )
                    val receivers = failSubscription.users.split(",").map {
                        EnvUtils.parseEnv(
                            command = it,
                            data = commandContext.variables,
                            replaceWithEmpty = true
                        )
                    }.toSet()
                    sendNotifyByTemplate(
                        templateCode = getNotifyTemplateCode(shutdownType, failSubscription.detailFlag),
                        receivers = receivers,
                        notifyType = failSubscription.types.map { it.name }.toMutableSet(),
                        titleParams = params,
                        bodyParams = params
                    )
                }
            }
            else -> Result<Any>(0)
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
