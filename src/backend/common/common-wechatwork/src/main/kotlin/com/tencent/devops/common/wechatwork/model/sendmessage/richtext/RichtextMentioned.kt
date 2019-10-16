package com.tencent.devops.common.wechatwork.model.sendmessage.richtext

import com.tencent.devops.common.wechatwork.model.enums.RichtextContentType

data class RichtextMentioned(
    val mentioned: RichtextMentionedMentioned = RichtextMentionedMentioned()
) : RichtextContent(RichtextContentType.mentioned)
