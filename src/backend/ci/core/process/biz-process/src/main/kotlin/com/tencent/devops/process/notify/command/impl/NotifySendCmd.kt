package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
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

    protected fun getNotifyTemplateCode(
        type: Int,
        detailFlag: Boolean,
        channelCode: ChannelCode?
    ): String {
        // 创作流渠道默认使用详情模板
        val useDetailTemplate = detailFlag || channelCode == ChannelCode.CREATIVE_STREAM
        return if (useDetailTemplate) {
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

    companion object {
        /**
         * 是否按取消通知处理。
         *
         * 手动取消直接按取消处理；超时终态仅在取消订阅至少配置了一个通知类型时按取消处理，否则仍走失败通知。
         * 通知分支选择与取消变量注入（PipelineNotifyService）共用本判据，避免两处口径不一致。
         *
         * @param setting 流水线设置，用于判断取消订阅是否配置了通知类型
         * @param buildStatus 本次构建的终态
         */
        fun isCancelNotify(setting: PipelineSetting, buildStatus: BuildStatus): Boolean {
            return buildStatus.isCancel() ||
                (buildStatus.isTimeout() && setting.cancelSubscriptionList?.any { it.types.isNotEmpty() } == true)
        }
    }
}
