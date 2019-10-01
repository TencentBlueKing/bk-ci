package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.NotificationResponseWithPage
import com.tencent.devops.notify.model.WechatNotifyMessage
import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation

interface WechatService {
    fun sendMqMsg(message: WechatNotifyMessage)

    fun sendMessage(wechatNotifyMessageWithOperation: WechatNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<WechatNotifyMessageWithOperation>
}