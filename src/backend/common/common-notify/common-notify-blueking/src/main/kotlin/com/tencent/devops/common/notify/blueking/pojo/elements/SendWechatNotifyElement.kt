package com.tencent.devops.common.notify.blueking.pojo.elements

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("发送微信通知", description = SendWechatNotifyElement.classType)
data class SendWechatNotifyElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "发送Wechat微信通知",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("接收人集合", required = true)
    val receivers: Set<String> = setOf(),
    @ApiModelProperty("通知内容", required = true)
    val body: String = "",
    @ApiModelProperty("通知内容带上流水线详情连接", required = true)
    val detailFlag: Boolean?
) : Element(name, id, status) {
    companion object {
        const val classType = "sendWechatNotify"
    }

    override fun getTaskAtom() = "wechatTaskAtom"

    override fun getClassType() = classType

    private fun getReceiverStr(receivers: Set<String>): String {
        val str = StringBuilder(0)
        receivers.forEach {
            // 第一个不加逗号隔开
            if (str.isNotEmpty()) str.append(",")
            str.append(it)
        }
        return str.toString()
    }
}
