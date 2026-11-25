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
package com.tencent.devops.notify.model

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.notify.constant.NotifyMQ.NOTIFY_WEWORK
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCard

@Suppress("ALL")
@Event(NOTIFY_WEWORK)
class WeworkNotifyMessageWithOperation : WechatNotifyMessage() {
    var id: String? = null
    var retryCount: Int = 0
    var lastError: String? = null
    /** 兼容单卡发送；审核新链路请用 receiverTemplateCards */
    var templateCard: WeworkTemplateCard? = null
    /** 按接收人拆分的审核卡片。非空时每人独立发送，失败只给该人降级文本 */
    var receiverTemplateCards: Map<String, WeworkTemplateCard>? = null

    override fun toString(): String {
        return String.format(
            "id(%s), retryCount(%s), hasCard(%s), receiverCards(%s), message(%s) ",
            id, retryCount, templateCard != null, receiverTemplateCards?.size ?: 0, super.toString()
        )
    }
}
