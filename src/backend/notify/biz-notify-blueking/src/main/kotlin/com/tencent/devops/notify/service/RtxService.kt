package com.tencent.devops.notify.service

import com.tencent.devops.notify.model.RtxNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.RtxNotifyMessage

interface RtxService {
    fun sendMqMsg(message: RtxNotifyMessage)

    fun sendMessage(rtxNotifyMessageWithOperation: RtxNotifyMessageWithOperation)

    fun listByCreatedTime(page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): NotificationResponseWithPage<RtxNotifyMessageWithOperation>
}