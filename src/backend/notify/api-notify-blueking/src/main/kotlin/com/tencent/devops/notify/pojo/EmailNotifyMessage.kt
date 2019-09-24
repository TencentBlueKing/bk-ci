package com.tencent.devops.notify.model

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
    private val receivers: MutableSet<String> = mutableSetOf()
    @ApiModelProperty("邮件抄送接收者")
    private val cc: MutableSet<String> = mutableSetOf()
    @ApiModelProperty("邮件密送接收者")
    private val bcc: MutableSet<String> = mutableSetOf()
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
        return receivers.toSet()
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
        return bcc.toSet()
    }

    fun clearCc() {
        cc.clear()
    }

    fun getCc(): Set<String> {
        return cc.toSet()
    }

    override fun toString(): String {
        return String.format("title(%s), sender(%s), receivers(%s), cc(%s), bcc(%s), body(email html do not show) ",
                title, sender, receivers, cc, bcc/*, body*/)
    }
}
