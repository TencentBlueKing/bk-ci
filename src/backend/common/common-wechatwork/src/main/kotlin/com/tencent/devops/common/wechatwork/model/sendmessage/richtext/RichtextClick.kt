package com.tencent.devops.common.wechatwork.model.sendmessage.richtext

import com.tencent.devops.common.wechatwork.model.enums.RichtextContentType

data class RichtextClick(
    val link: RichtextClickLink = RichtextClickLink()
) : RichtextContent(RichtextContentType.link)
