package com.tencent.devops.notify.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.service.NotifyMessageTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceNotifyMessageTemplateResourceImpl @Autowired constructor(
    private val notifyMessageTemplateService: NotifyMessageTemplateService
) : ServiceNotifyMessageTemplateResource {

    override fun sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest): Result<Boolean> {
        return notifyMessageTemplateService.sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
    }
}