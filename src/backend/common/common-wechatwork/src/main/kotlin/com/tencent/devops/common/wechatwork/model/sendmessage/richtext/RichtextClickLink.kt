package com.tencent.devops.common.wechatwork.model.sendmessage.richtext

import com.tencent.devops.common.wechatwork.model.enums.RichtextLinkType

data class RichtextClickLink(
    val text: String = "",
    val key: String = ""
) {
    val type: RichtextLinkType = RichtextLinkType.click
}
