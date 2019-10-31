package com.tencent.devops.notify.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.notify.api.op.OpNotifyMessageTemplateResource
import com.tencent.devops.notify.model.NotifyMessageCommonTemplate
import com.tencent.devops.notify.model.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.SubNotifyMessageTemplate
import com.tencent.devops.notify.service.NotifyMessageTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpNotifyMessageTemplateResourceImpl @Autowired constructor(
    private val notifyMessageTemplateService: NotifyMessageTemplateService
) : OpNotifyMessageTemplateResource {

    override fun getNotifyMessageTemplates(userId: String, templateId: String): Result<Page<SubNotifyMessageTemplate>> {
        return notifyMessageTemplateService.getNotifyMessageTemplates(userId, templateId)
    }

    override fun getCommonNotifyMessageTemplates(userId: String, templateCode: String?, templateName: String?, page: Int?, pageSize: Int?): Result<Page<NotifyMessageCommonTemplate>> {
        return notifyMessageTemplateService.getCommonNotifyMessageTemplates(userId, templateCode, templateName, page, pageSize)
    }

    override fun addNotifyMessageTemplate(userId: String, notifyMessageTemplateRequest: NotifyTemplateMessageRequest): Result<Boolean> {
        return notifyMessageTemplateService.addNotifyMessageTemplate(userId, notifyMessageTemplateRequest)
    }

    override fun updateNotifyMessageTemplate(
        userId: String,
        templateId: String,
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean> {
        return notifyMessageTemplateService.updateNotifyMessageTemplate(userId, templateId, notifyMessageTemplateRequest)
    }

    override fun deleteNotifyMessageTemplate(templateId: String, notifyType: String): Result<Boolean> {
        return notifyMessageTemplateService.deleteNotifyMessageTemplate(templateId, notifyType)
    }

    override fun deleteCommonNotifyMessageTemplate(templateId: String): Result<Boolean> {
        return notifyMessageTemplateService.deleteCommonNotifyMessageTemplate(templateId)
    }
}