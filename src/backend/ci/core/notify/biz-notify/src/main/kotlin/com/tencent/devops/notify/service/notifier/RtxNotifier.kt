package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.service.WeworkService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RtxNotifier @Autowired constructor(
    private val weworkService: WeworkService,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val dslContext: DSLContext
) : INotifier {
    @Value("\${wework.domain}")
    private val userUseDomain: Boolean = true
    override fun type(): NotifyType = NotifyType.RTX
    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        logger.info("send wework msg: ${commonNotifyMessageTemplateRecord.id}")
        val weworkTplRecord =
            notifyMessageTemplateDao.getRtxNotifyMessageTemplate(
                dslContext = dslContext,
                commonTemplateId = commonNotifyMessageTemplateRecord.id
            )!!
        val title = NotifierUtils.replaceContentParams(request.titleParams, weworkTplRecord.title)
        // 替换内容里的动态参数
        val body = NotifierUtils.replaceContentParams(
            request.bodyParams,
            if (request.markdownContent == true) {
                weworkTplRecord.bodyMd ?: weworkTplRecord.body
            } else {
                weworkTplRecord.body
            }
        )
        logger.info("send wework msg: $body ${weworkTplRecord.sender}")
        NotifierUtils.sendWeworkNotifyMessage(
            commonNotifyMessageTemplate = commonNotifyMessageTemplateRecord,
            sendNotifyMessageTemplateRequest = request,
            body = "$title\n\n$body",
            sender = weworkTplRecord.sender,
            weworkService = weworkService,
            userUseDomain = userUseDomain
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RtxNotifier::class.java)
    }
}
