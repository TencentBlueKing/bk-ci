package com.tencent.devops.common.wechatwork.model.sendmessage.richtext

import com.tencent.devops.common.wechatwork.model.enums.RichtextContentType

data class RichtextView(
    val link: RichtextViewLink = RichtextViewLink()
) : RichtextContent(RichtextContentType.link)
