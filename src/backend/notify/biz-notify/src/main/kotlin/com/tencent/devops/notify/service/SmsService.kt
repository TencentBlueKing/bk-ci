package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.SmsNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.SmsNotifyMessage

interface SmsService {
    fun sendMqMsg(message: SmsNotifyMessage)

    fun sendMessage(smsNotifyMessageWithOperation: SmsNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<SmsNotifyMessageWithOperation>
}