package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.NotificationResponseWithPage

interface EmailService {
    fun sendMqMsg(message: EmailNotifyMessage)

    fun sendMessage(emailNotifyMessageWithOperation: EmailNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<EmailNotifyMessageWithOperation>
}