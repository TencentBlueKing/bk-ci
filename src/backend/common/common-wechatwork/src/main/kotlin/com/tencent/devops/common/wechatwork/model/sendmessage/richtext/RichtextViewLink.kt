package com.tencent.devops.common.wechatwork.model.sendmessage.richtext

import com.tencent.devops.common.wechatwork.model.enums.RichtextLinkType

data class RichtextViewLink(
    val text: String = "",
    val key: String = "",
    val browser: Int = 0
) {

    val type: RichtextLinkType = RichtextLinkType.view
}
