package com.tencent.devops.notify.blueking.service

import com.tencent.devops.notify.blueking.model.EmailNotifyMessage
import com.tencent.devops.notify.blueking.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.blueking.model.NotificationResponseWithPage

interface EmailService {
    fun sendMqMsg(message: EmailNotifyMessage)

    fun sendMessage(emailNotifyMessageWithOperation: EmailNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<EmailNotifyMessageWithOperation>
}