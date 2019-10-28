package com.tencent.devops.common.wechatwork.model.sendmessage.richtext
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver

data class RichtextMessage(
    val receiver: Receiver = Receiver(),
    val rich_text: List<RichtextContent> = listOf(RichtextText())
) {
    val msgtype: String = "rich_text"
}
