package com.tencent.devops.process.notify.command.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.notify.command.NotifyCmd

abstract class NotifySendCmd(val client: Client) : NotifyCmd {

    protected fun sendNotifyByTemplate(
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
}
