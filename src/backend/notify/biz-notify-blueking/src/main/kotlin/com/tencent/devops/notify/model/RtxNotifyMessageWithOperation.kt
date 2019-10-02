package com.tencent.devops.notify.model

import com.tencent.devops.notify.pojo.RtxNotifyMessage

class RtxNotifyMessageWithOperation : RtxNotifyMessage() {
    var id: String? = null
    var batchId: String? = null
    var retryCount: Int = 0
    var lastError: String? = null

    override fun toString(): String {
        return String.format(
            "id(%s), batchId(%s), retryCount(%s), message(%s) ",
            id, batchId, retryCount, super.toString()
        )
    }
}