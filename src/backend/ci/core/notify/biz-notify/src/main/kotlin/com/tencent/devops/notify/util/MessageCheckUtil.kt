/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
@Suppress("ALL")
object MessageCheckUtil {

    private fun checkTitle(title: String?) {
        if (title.isNullOrBlank()) {
            throw InvalidParamException(
                message = "invalid title:$title",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_TITLE,
                params = arrayOf(title ?: "")
            )
        }
    }

    private fun checkBody(body: String?) {
        if (body.isNullOrBlank()) {
            throw InvalidParamException(
                message = "invalid body:$body",
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
        //        群通知receiver可能为空
        //        checkReceivers(message.getReceivers())
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
