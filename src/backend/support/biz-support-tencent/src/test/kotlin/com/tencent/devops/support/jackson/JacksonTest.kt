package com.tencent.devops.support.jackson

import com.tencent.devops.common.api.util.JacksonUtil
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextClick
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextClickLink
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMentioned
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMentionedMentioned
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink
import org.junit.Ignore
import org.junit.Test

class JacksonTest {

    @Ignore
    @Test
    fun test() {
        val richtextContentList = mutableListOf<RichtextContent>()
        val richtextText = RichtextText(RichtextTextText("asdfhsadkljfhksladf奥斯卡接电话反馈就暗示的合法撒旦法律！！！"))
        richtextContentList.add(richtextText)
        val richtextMentioned = RichtextMentioned(RichtextMentionedMentioned(listOf("freyzheng")))
        richtextContentList.add(richtextMentioned)
        val richtextClick = RichtextClick(RichtextClickLink(text = "赞成", key = "sadfsdfdasf"))
        richtextContentList.add(richtextClick)
        val richtextView = RichtextView(RichtextViewLink(text = "赞成", key = "km.oa.com", browser = 1))
        richtextContentList.add(richtextView)

        val objectMapper = JacksonUtil.createObjectMapper()
        var str: String
        str = objectMapper.writeValueAsString(richtextText)
        System.out.println(str)
        str = objectMapper.writeValueAsString(richtextMentioned)
        System.out.println(str)
        str = objectMapper.writeValueAsString(richtextClick)
        System.out.println(str)
        str = objectMapper.writeValueAsString(richtextView)
        System.out.println(str)

        str = objectMapper.writeValueAsString(richtextContentList)
        System.out.println(str)
    }
}
