/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("email电子邮件消息类型")
open class EmailNotifyMessage : BaseMessage() {

    @ApiModelProperty("邮件格式", allowableValues = "0,1", dataType = "int")
    var format: EnumEmailFormat = EnumEmailFormat.PLAIN_TEXT

    @ApiModelProperty("邮件类型", allowableValues = "0,1", dataType = "int")
    var type: EnumEmailType = EnumEmailType.OUTER_MAIL

    @ApiModelProperty("通知接收者")
    private val receivers: LinkedHashSet<String> = LinkedHashSet()

    @ApiModelProperty("邮件抄送接收者")
    private val cc: LinkedHashSet<String> = LinkedHashSet()

    @ApiModelProperty("邮件密送接收者")
    private val bcc: LinkedHashSet<String> = LinkedHashSet()

    @ApiModelProperty("邮件内容")
    var body: String = ""

    @ApiModelProperty("邮件发送者")
    var sender: String = "DevOps"

    @ApiModelProperty("邮件标题")
    var title: String = ""

    @ApiModelProperty("优先级", allowableValues = "-1,1,1", dataType = "int")
    var priority: EnumNotifyPriority = EnumNotifyPriority.HIGH

    @ApiModelProperty("通知来源", allowableValues = "0,1", dataType = "int")
    var source: EnumNotifySource = EnumNotifySource.BUSINESS_LOGIC

    @ApiModelProperty("codecc邮件附件内容")
    var codeccAttachFileContent: Map<String, String>? = mapOf()

    @ApiModelProperty("邮件内容，可替代的上下文集合[腾讯云邮件服务只支持传模板参数形式]")
    var variables: Map<String, String>? = mapOf()

    @ApiModelProperty("腾讯云邮件模板id")
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

    @ApiModelProperty(hidden = true)
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
