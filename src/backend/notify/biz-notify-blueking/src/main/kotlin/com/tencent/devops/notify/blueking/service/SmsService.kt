package com.tencent.devops.notify.blueking.service

import com.tencent.devops.notify.blueking.model.NotificationResponseWithPage
import com.tencent.devops.notify.blueking.model.SmsNotifyMessage
import com.tencent.devops.notify.blueking.model.SmsNotifyMessageWithOperation

interface SmsService {
    fun sendMqMsg(message: SmsNotifyMessage)

    fun sendMessage(smsNotifyMessageWithOperation: SmsNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<SmsNotifyMessageWithOperation>
}