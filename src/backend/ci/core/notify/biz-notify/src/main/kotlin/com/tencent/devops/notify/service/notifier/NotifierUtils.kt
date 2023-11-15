package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.service.WeworkService

object NotifierUtils {
    fun sendWeworkNotifyMessage(
        commonNotifyMessageTemplate: TCommonNotifyMessageTemplateRecord,
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest,
        body: String,
        sender: String,
        weworkService: WeworkService,
        userUseDomain: Boolean
    ) {
        val wechatNotifyMessage = WeworkNotifyMessageWithOperation()
        wechatNotifyMessage.sender = sender
        wechatNotifyMessage.addAllReceivers(findWeworkUser(sendNotifyMessageTemplateRequest.receivers, userUseDomain))
        wechatNotifyMessage.body = body
        wechatNotifyMessage.priority = EnumNotifyPriority.parse(commonNotifyMessageTemplate.priority.toString())
        wechatNotifyMessage.source = EnumNotifySource.parse(commonNotifyMessageTemplate.source.toInt())
            ?: EnumNotifySource.BUSINESS_LOGIC
        wechatNotifyMessage.markdownContent = sendNotifyMessageTemplateRequest.markdownContent ?: false
        weworkService.sendMqMsg(wechatNotifyMessage)
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
