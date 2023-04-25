/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class JacksonTest {

    @Disabled
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
