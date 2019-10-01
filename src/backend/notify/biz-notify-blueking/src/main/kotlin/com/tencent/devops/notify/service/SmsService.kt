package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.NotificationResponseWithPage
import com.tencent.devops.notify.model.SmsNotifyMessage
import com.tencent.devops.notify.model.SmsNotifyMessageWithOperation

interface SmsService {
    fun sendMqMsg(message: SmsNotifyMessage)

    fun sendMessage(smsNotifyMessageWithOperation: SmsNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<SmsNotifyMessageWithOperation>
}