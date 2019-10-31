package com.tencent.devops.notify.blueking.service

import com.tencent.devops.notify.blueking.model.NotificationResponseWithPage
import com.tencent.devops.notify.blueking.model.RtxNotifyMessage
import com.tencent.devops.notify.blueking.model.RtxNotifyMessageWithOperation

interface RtxService {
    fun sendMqMsg(message: RtxNotifyMessage)

    fun sendMessage(rtxNotifyMessageWithOperation: RtxNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<RtxNotifyMessageWithOperation>
}