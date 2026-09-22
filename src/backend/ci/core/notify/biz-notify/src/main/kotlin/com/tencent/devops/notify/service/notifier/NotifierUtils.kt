package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.service.WeworkService
import org.slf4j.LoggerFactory

object NotifierUtils {
    private val logger = LoggerFactory.getLogger(NotifierUtils::class.java)

    fun sendWeworkNotifyMessage(
        commonNotifyMessageTemplate: TCommonNotifyMessageTemplateRecord,
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest,
        body: String,
        sender: String,
        weworkService: WeworkService,
        userUseDomain: Boolean,
        templateCard: com.tencent.devops.notify.pojo.wework.WeworkTemplateCard? = null,
        receiverTemplateCards: Map<String, com.tencent.devops.notify.pojo.wework.WeworkTemplateCard>? = null
    ) {
        val wechatNotifyMessage = WeworkNotifyMessageWithOperation()
        wechatNotifyMessage.sender = sender
        wechatNotifyMessage.addAllReceivers(findWeworkUser(sendNotifyMessageTemplateRequest.receivers, userUseDomain))
        wechatNotifyMessage.body = body
        wechatNotifyMessage.priority = EnumNotifyPriority.parse(commonNotifyMessageTemplate.priority.toString())
        wechatNotifyMessage.source = EnumNotifySource.parse(commonNotifyMessageTemplate.source.toInt())
            ?: EnumNotifySource.BUSINESS_LOGIC
        wechatNotifyMessage.markdownContent = sendNotifyMessageTemplateRequest.markdownContent ?: false
        val remappedCards = remapReceiverCards(receiverTemplateCards, userUseDomain)
        // 有按人拆卡时不写 templateCard，避免滚动发布时旧消费者把同一张卡群发给所有人
        wechatNotifyMessage.receiverTemplateCards = remappedCards
        wechatNotifyMessage.templateCard = if (remappedCards.isNullOrEmpty()) templateCard else null
        logger.info(
            "reviewNotifyTrace|hop=notify.mq|" +
                "template=${sendNotifyMessageTemplateRequest.templateCode}|" +
                "sender=$sender|receivers=${JsonUtil.toJson(wechatNotifyMessage.getReceivers())}|" +
                "markdown=${wechatNotifyMessage.markdownContent}|hasCard=${templateCard != null}|" +
                "receiverCards=${remappedCards?.size ?: 0}|" +
                "taskId=${templateCard?.taskId}|body=$body"
        )
        weworkService.sendMqMsg(wechatNotifyMessage)
    }

    private fun remapReceiverCards(
        receiverTemplateCards: Map<String, com.tencent.devops.notify.pojo.wework.WeworkTemplateCard>?,
        userUseDomain: Boolean
    ): Map<String, com.tencent.devops.notify.pojo.wework.WeworkTemplateCard>? {
        if (receiverTemplateCards.isNullOrEmpty()) {
            return receiverTemplateCards
        }
        val remapped = linkedMapOf<String, com.tencent.devops.notify.pojo.wework.WeworkTemplateCard>()
        receiverTemplateCards.forEach { (raw, card) ->
            val key = if (userUseDomain && raw.contains("@")) raw.substringBefore("@") else raw
            remapped[key] = card
        }
        return remapped
    }

    /**
     * 根据当前请求渠道对通知内容做关键字替换。
     * 当渠道为 CREATIVE_STREAM 时，将文本中的「流水线」替换为「创作流」（国际化）。
     *
     * @param content 待处理的文本
     * @param language 语言标识（如 zh_CN），用于从国际化资源中获取关键字本地化文本
     * @return 替换后的文本
     */
    fun replaceNotifyKeywordByChannel(content: String, language: String): String {
        return MessageUtil.replaceKeywordByChannel(
            message = content,
            language = language
        ) ?: content
    }

    fun replaceContentParams(
        params: Map<String, String>?,
        content: String,
        action: (String) -> String = { it }
    ): String {
        var contentTmp = content
        params?.forEach { (paramName, paramValue) ->
            val replaceValue = action(paramValue)
            contentTmp = contentTmp.replace("\${$paramName}", replaceValue).replace("#{$paramName}", replaceValue)
                .replace("{{$paramName}}", replaceValue)
        }
        return contentTmp
    }

    // #5318 为解决使用蓝鲸用户中心生成了带域名的用户名无法与企业微信账号对齐问题
    private fun findWeworkUser(userSet: Set<String>, userUseDomain: Boolean): Set<String> {
        if (userUseDomain) {
            val weworkUserSet = mutableSetOf<String>()
            userSet.forEach {
                // 若用户名包含域,取域前的用户名.
                if (it.contains("@")) {
                    weworkUserSet.add(it.substringBefore("@"))
                } else {
                    weworkUserSet.add(it)
                }
            }
            return weworkUserSet
        }
        return userSet
    }
}
