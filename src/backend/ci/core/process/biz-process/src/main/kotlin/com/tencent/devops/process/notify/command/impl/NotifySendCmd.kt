package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.notify.command.NotifyCmd
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum

abstract class NotifySendCmd(val client: Client) : NotifyCmd {

    protected fun sendNotifyByTemplate(
        templateCode: String,
        receivers: Set<String>,
        notifyType: Set<String>,
        titleParams: Map<String, String>,
        bodyParams: Map<String, String>,
        markdownContent: Boolean
    ) {
        client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
            SendNotifyMessageTemplateRequest(
                templateCode = templateCode,
                receivers = receivers as MutableSet<String>,
                notifyType = notifyType as MutableSet<String>,
                titleParams = titleParams,
                bodyParams = bodyParams,
                cc = null,
                bcc = null,
                markdownContent = markdownContent
            )
        )
    }

    protected fun getNotifyTemplateCode(type: Int, detailFlag: Boolean): String {
        return if (detailFlag) {
            when (type) {
                BluekingNotifySendCmd.TYPE_STARTUP ->
                    PipelineNotifyTemplateEnum.PIPELINE_STARTUP_NOTIFY_TEMPLATE_DETAIL.templateCode
                BluekingNotifySendCmd.TYPE_SHUTDOWN_SUCCESS ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE_DETAIL.templateCode
                BluekingNotifySendCmd.TYPE_SHUTDOWN_FAILURE ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE_DETAIL.templateCode
                BluekingNotifySendCmd.TYPE_SHUTDOWN_CANCEL ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE_DETAIL.templateCode
                else ->
                    throw IllegalArgumentException("Unknown type($type) of Notify")
            }
        } else {
            when (type) {
                BluekingNotifySendCmd.TYPE_STARTUP ->
                    PipelineNotifyTemplateEnum.PIPELINE_STARTUP_NOTIFY_TEMPLATE.templateCode
                BluekingNotifySendCmd.TYPE_SHUTDOWN_SUCCESS ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE.templateCode
                BluekingNotifySendCmd.TYPE_SHUTDOWN_FAILURE ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE.templateCode
                BluekingNotifySendCmd.TYPE_SHUTDOWN_CANCEL ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE.templateCode
                else ->
                    throw IllegalArgumentException("Unknown type($type) of Notify")
            }
        }
    }
}
