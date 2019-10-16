package com.tencent.devops.common.wechatwork.model.sendmessage.richtext

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.tencent.devops.common.wechatwork.model.enums.RichtextContentType

@JsonSubTypes(
        JsonSubTypes.Type(value = RichtextText::class, name = "text"),
        JsonSubTypes.Type(value = RichtextMentioned::class, name = "mentioned"),
        JsonSubTypes.Type(value = RichtextClick::class, name = "click"),
        JsonSubTypes.Type(value = RichtextView::class, name = "view")
)
abstract class RichtextContent(
    val type: RichtextContentType
)