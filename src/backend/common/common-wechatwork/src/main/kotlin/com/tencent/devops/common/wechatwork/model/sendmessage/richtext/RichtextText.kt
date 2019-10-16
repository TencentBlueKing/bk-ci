package com.tencent.devops.common.wechatwork.model.sendmessage.richtext

import com.tencent.devops.common.wechatwork.model.enums.RichtextContentType

data class RichtextText(
    val text: RichtextTextText = RichtextTextText()
) : RichtextContent(RichtextContentType.text)
