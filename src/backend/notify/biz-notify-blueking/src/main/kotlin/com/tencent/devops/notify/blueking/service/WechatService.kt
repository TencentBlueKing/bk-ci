package com.tencent.devops.notify.blueking.service

import com.tencent.devops.notify.blueking.model.NotificationResponseWithPage
import com.tencent.devops.notify.blueking.model.WechatNotifyMessage
import com.tencent.devops.notify.blueking.model.WechatNotifyMessageWithOperation

interface WechatService {
    fun sendMqMsg(message: WechatNotifyMessage)

    fun sendMessage(wechatNotifyMessageWithOperation: WechatNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<WechatNotifyMessageWithOperation>
}