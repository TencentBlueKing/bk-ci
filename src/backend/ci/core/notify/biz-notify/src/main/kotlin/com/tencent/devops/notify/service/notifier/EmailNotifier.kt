package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.service.EmailService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EmailNotifier @Autowired constructor(
    private val emailService: EmailService,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val dslContext: DSLContext
) : INotifier {
    override fun type(): NotifyType = NotifyType.EMAIL
    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        val emailTplRecord = notifyMessageTemplateDao.getEmailNotifyMessageTemplate(
            dslContext,
            commonNotifyMessageTemplateRecord.id
        )!!
        // 替换标题里的动态参数
        val title = NotifierUtils.replaceContentParams(request.titleParams, emailTplRecord.title)
        // 替换内容里的动态参数
        val body = NotifierUtils.replaceContentParams(request.bodyParams, emailTplRecord.body) {
            it.replace("\n", "<br>")
        }
        sendEmailNotifyMessage(
            commonNotifyMessageTemplate = commonNotifyMessageTemplateRecord,
            sendNotifyMessageTemplateRequest = request,
            title = title,
            body = body,
            sender = emailTplRecord.sender,
            variables = request.titleParams?.plus(request.bodyParams ?: emptyMap()) ?: emptyMap(),
            tencentCloudTemplateId = emailTplRecord.tencentCloudTemplateId
        )
    }

    private fun sendEmailNotifyMessage(
        commonNotifyMessageTemplate: TCommonNotifyMessageTemplateRecord,
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest,
        title: String,
        body: String,
        sender: String,
        variables: Map<String, String>,
        tencentCloudTemplateId: Int?
    ) {
        logger.info("sendEmailNotifyMessage:\ntitle:$title,\nbody:$body")
        val commonTemplateId = commonNotifyMessageTemplate.id
        val emailNotifyMessageTemplate =
            notifyMessageTemplateDao.getEmailNotifyMessageTemplate(dslContext, commonTemplateId)
        val emailNotifyMessage = EmailNotifyMessage()
        emailNotifyMessage.sender = sender
        emailNotifyMessage.addAllReceivers(sendNotifyMessageTemplateRequest.receivers)
        val cc = sendNotifyMessageTemplateRequest.cc
        if (null != cc) {
            emailNotifyMessage.addAllCcs(cc)
        }
        val bcc = sendNotifyMessageTemplateRequest.bcc
        if (null != bcc) {
            emailNotifyMessage.addAllBccs(bcc)
        }
        emailNotifyMessage.title = title
        emailNotifyMessage.body = body
        emailNotifyMessage.variables = variables
        emailNotifyMessage.tencentCloudTemplateId = tencentCloudTemplateId
        emailNotifyMessage.priority = EnumNotifyPriority.parse(commonNotifyMessageTemplate.priority.toString())
        emailNotifyMessage.source = EnumNotifySource.parse(commonNotifyMessageTemplate.source.toInt())
            ?: EnumNotifySource.BUSINESS_LOGIC
        emailNotifyMessage.format = EnumEmailFormat.parse(emailNotifyMessageTemplate!!.bodyFormat.toInt())
        emailNotifyMessage.type = EnumEmailType.parse(emailNotifyMessageTemplate.emailType.toInt())
        emailService.sendMqMsg(emailNotifyMessage)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EmailNotifier::class.java)
    }
}
