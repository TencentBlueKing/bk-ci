package com.tencent.devops.notify.util

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.notify.constant.NotifyMessageCode
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage

/**
 * @Description
 * @Date 2019/11/15
 * @Version 1.0
 */
object MessageCheckUtil {

    private fun checkTitle(title: String?) {
        if (title.isNullOrBlank()) {
            throw InvalidParamException(
                message = "invalid title:${title}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_TITLE,
                params = arrayOf(title ?: "")
            )
        }
    }

    private fun checkBody(body: String?) {
        if (body.isNullOrBlank()) {
            throw InvalidParamException(
                message = "invalid body:${body}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_BODY,
                params = arrayOf(body ?: "")
            )
        }
    }

    private fun checkReceivers(receivers: Set<String>) {
        if (receivers.isEmpty()) {
            throw InvalidParamException(
                message = "invalid receivers:$receivers",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_RECEIVERS,
                params = arrayOf("$receivers")
            )
        }
    }

    fun checkRtxMessage(message: RtxNotifyMessage) {
        checkTitle(message.title)
        checkBody(message.body)
        checkReceivers(message.getReceivers())
    }

    fun checkEmailMessage(message: EmailNotifyMessage) {
        checkTitle(message.title)
        checkBody(message.body)
        checkReceivers(message.getReceivers())
    }

    fun checkWechatMessage(message: WechatNotifyMessage) {
        checkBody(message.body)
        checkReceivers(message.getReceivers())
    }

    fun checkSmsMessage(message: SmsNotifyMessage) {
        checkBody(message.body)
        checkReceivers(message.getReceivers())
    }
}