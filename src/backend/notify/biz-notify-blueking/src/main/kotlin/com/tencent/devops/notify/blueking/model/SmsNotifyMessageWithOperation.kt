package com.tencent.devops.notify.blueking.model

class SmsNotifyMessageWithOperation : SmsNotifyMessage() {
    var id: String? = null
    var batchId: String? = null
    var retryCount: Int = 0
    var lastError: String? = null

    override fun toString(): String {
        return String.format("id(%s), batchId(%s), retryCount(%s), message(%s) ",
                id, batchId, retryCount, super.toString())
    }
}