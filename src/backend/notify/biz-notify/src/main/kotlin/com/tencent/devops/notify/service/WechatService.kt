package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.WechatNotifyMessage

interface WechatService {
    fun sendMqMsg(message: WechatNotifyMessage)

    fun sendMessage(wechatNotifyMessageWithOperation: WechatNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<WechatNotifyMessageWithOperation>
}