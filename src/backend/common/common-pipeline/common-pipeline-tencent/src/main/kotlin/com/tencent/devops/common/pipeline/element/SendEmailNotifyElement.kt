package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("发送邮件通知", description = SendEmailNotifyElement.classType)
data class SendEmailNotifyElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "发送Email通知",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("通知接收者", required = true)
    val receivers: Set<String> = setOf(),
    @ApiModelProperty("邮件抄送接收者")
    val cc: Set<String> = setOf(),
    @ApiModelProperty("Email通知标题", required = true)
    val title: String = "",
    @ApiModelProperty("Email通知内容", required = true)
    val body: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "sendEmailNotify"
    }

    override fun getTaskAtom() = "emailTaskAtom"

    override fun getClassType() = classType

    private fun getSetStr(receivers: Set<String>): String {
        val str = StringBuilder("")
        receivers.forEach {
            // 第一个不加逗号隔开
            if (str.isNotEmpty()) str.append(",")
            str.append(it)
        }
        return str.toString()
    }

    private fun getCcSetStr(ccSet: Set<String>): String {
        val str = StringBuilder("")
        ccSet.forEach {
            // 第一个不加逗号隔开
            if (str.isNotEmpty()) str.append(",")
            str.append(it)
        }
        val result = str.toString()
        if (result.isNotBlank()) return result
        return "##"
    }
}
