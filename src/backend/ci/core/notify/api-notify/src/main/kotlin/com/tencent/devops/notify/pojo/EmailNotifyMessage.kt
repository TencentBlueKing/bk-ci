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

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.notify.api.annotation.BkNotifyReceivers
import com.tencent.devops.notify.constant.NotifyMQ.NOTIFY_EMAIL
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "email电子邮件消息类型")
@Event(destination = NOTIFY_EMAIL)
open class EmailNotifyMessage(
    override var delayMills: Int = 0,
    override var retryTime: Int = 0
) : BaseMessage(delayMills, retryTime) {

    @get:Schema(title = "邮件格式", allowableValues = ["0", "1"], type = "int")
    var format: EnumEmailFormat = EnumEmailFormat.PLAIN_TEXT

    @get:Schema(title = "邮件类型", allowableValues = ["0", "1"], type = "int")
    var type: EnumEmailType = EnumEmailType.OUTER_MAIL

    @get:Schema(title = "通知接收者")
    @BkNotifyReceivers
    private val receivers: LinkedHashSet<String> = LinkedHashSet()

    @get:Schema(title = "邮件抄送接收者")
    private val cc: LinkedHashSet<String> = LinkedHashSet()

    @get:Schema(title = "邮件密送接收者")
    private val bcc: LinkedHashSet<String> = LinkedHashSet()

    @get:Schema(title = "邮件内容")
    var body: String = ""

    @get:Schema(title = "邮件发送者")
    var sender: String = "DevOps"

    @get:Schema(title = "邮件标题")
    var title: String = ""

    @get:Schema(title = "优先级", allowableValues = ["-1", "0", "1"], type = "int")
    var priority: EnumNotifyPriority = EnumNotifyPriority.HIGH

    @get:Schema(title = "通知来源", allowableValues = ["0", "1"], type = "int")
    var source: EnumNotifySource = EnumNotifySource.BUSINESS_LOGIC

    @get:Schema(title = "codecc邮件附件内容")
    var codeccAttachFileContent: Map<String, String>? = mapOf()

    @get:Schema(title = "邮件内容，可替代的上下文集合[腾讯云邮件服务只支持传模板参数形式]")
    var variables: Map<String, String>? = mapOf()

    @get:Schema(title = "腾讯云邮件模板id")
    var tencentCloudTemplateId: Int? = null

    fun addReceiver(receiver: String) {
        receivers.add(receiver)
    }

    fun addAllReceivers(receiverSet: Set<String>) {
        receivers.addAll(receiverSet)
    }

    fun addCc(ccSingle: String) {
        cc.add(ccSingle)
    }

    fun addAllCcs(ccSet: Set<String>) {
        cc.addAll(ccSet)
    }

    fun addBcc(bccSingle: String) {
        bcc.add(bccSingle)
    }

    fun addAllBccs(bccSet: Set<String>) {
        bcc.addAll(bccSet)
    }

    fun clearReceivers() {
        receivers.clear()
    }

    fun getReceivers(): Set<String> {
        return receivers
    }

    @Schema(hidden = true)
    fun isReceiversEmpty(): Boolean {
        if (receivers.size == 0) return true
        return false
    }

    fun clearBcc() {
        bcc.clear()
    }

    fun getBcc(): Set<String> {
        return bcc
    }

    fun clearCc() {
        cc.clear()
    }

    fun getCc(): Set<String> {
        return cc
    }

    override fun toString(): String {
        return String.format(
            "title(%s), sender(%s), receivers(%s), cc(%s), bcc(%s), body(email html do not show) ",
            title, sender, receivers, cc, bcc/*, body*/
        )
    }
}
