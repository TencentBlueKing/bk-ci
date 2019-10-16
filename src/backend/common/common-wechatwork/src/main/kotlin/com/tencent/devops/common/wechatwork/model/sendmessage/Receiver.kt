package com.tencent.devops.common.wechatwork.model.sendmessage

import com.tencent.devops.common.wechatwork.model.enums.ReceiverType

data class Receiver(
    val type: ReceiverType = ReceiverType.single,
    val id: String = ""
)
