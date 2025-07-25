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
package com.tencent.devops.notify.pojo

import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.notify.api.annotation.BkNotifyReceivers
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "wechat微信消息类型")
open class WechatNotifyMessage : BaseMessage() {

    @get:Schema(title = "通知接收者")
    @BkNotifyReceivers
    private val receivers: MutableSet<String> = mutableSetOf()
    @get:Schema(title = "通知内容")
    var body: String = ""
    @get:Schema(title = "通知发送者")
    var sender: String = ""
    @get:Schema(title = "优先级", allowableValues = ["-1", "0", "1"], type = "int")
    var priority: EnumNotifyPriority = EnumNotifyPriority.HIGH
    @get:Schema(title = "通知来源", allowableValues = ["0", "1"], type = "int")
    var source: EnumNotifySource = EnumNotifySource.BUSINESS_LOGIC
    @get:Schema(title = "是否markdown")
    var markdownContent: Boolean = false

    fun addReceiver(receiver: String) {
        receivers.add(receiver)
    }

    fun addAllReceivers(receiverSet: Set<String>) {
        receivers.addAll(receiverSet)
    }

    fun clearReceivers() {
        receivers.clear()
    }

    fun getReceivers(): Set<String> {
        return receivers.toSet()
    }

    @Schema(hidden = true)
    fun isReceiversEmpty(): Boolean {
        if (receivers.size == 0) return true
        return false
    }

    override fun toString(): String {
        return String.format("sender(%s), receivers(%s), body(%s) ",
                sender, receivers, body)
    }
}
