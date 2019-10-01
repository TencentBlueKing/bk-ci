package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.NotificationResponseWithPage
import com.tencent.devops.notify.model.RtxNotifyMessage
import com.tencent.devops.notify.model.RtxNotifyMessageWithOperation

interface RtxService {
    fun sendMqMsg(message: RtxNotifyMessage)

    fun sendMessage(rtxNotifyMessageWithOperation: RtxNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<RtxNotifyMessageWithOperation>
}