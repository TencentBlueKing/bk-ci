package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
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
class WeworkNotifier @Autowired constructor(
    private val weworkService: WeworkService,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val dslContext: DSLContext,
    private val commonConfig: CommonConfig,
    private val redisOperation: RedisOperation
) : INotifier {
    @Value("\${wework.domain}")
    private val userUseDomain: Boolean = true
    override fun type(): NotifyType = NotifyType.WEWORK
    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        val weworkTplRecord = notifyMessageTemplateDao.getWeworkNotifyMessageTemplate(
            dslContext = dslContext,
            commonTemplateId = commonNotifyMessageTemplateRecord.id
        )!!
        // 先对 DB 原始模板做渠道关键字替换（如 CREATIVE_STREAM 渠道将「流水线」替换为「创作流」），再替换占位符
        val language = commonConfig.devopsDefaultLocaleLanguage
        val rawTitle = NotifierUtils.replaceNotifyKeywordByChannel(weworkTplRecord.title, language)
        val rawBody = NotifierUtils.replaceNotifyKeywordByChannel(weworkTplRecord.body, language)
        val finalTitle = NotifierUtils.replaceContentParams(request.titleParams, rawTitle)
        val finalBody = NotifierUtils.replaceContentParams(request.bodyParams, rawBody)

        // 人工审核 / Stage 审核：优先发送模板卡片（同意/拒绝 + 双端详情）
        val reviewCard = if (WeworkReviewCardBuilder.isReviewCardTemplate(request.templateCode)) {
            WeworkReviewCardBuilder.build(
                request = request,
                title = finalTitle,
                redisOperation = redisOperation
            )
        } else {
            null
        }
        val body = if (reviewCard != null) {
            reviewCard.second
        } else {
            "$finalTitle\n\n$finalBody"
        }
        NotifierUtils.sendWeworkNotifyMessage(
            commonNotifyMessageTemplate = commonNotifyMessageTemplateRecord,
            sendNotifyMessageTemplateRequest = request,
            body = body,
            sender = weworkTplRecord.sender,
            weworkService = weworkService,
            userUseDomain = userUseDomain,
            templateCard = reviewCard?.first
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WeworkNotifier::class.java)
    }
}
