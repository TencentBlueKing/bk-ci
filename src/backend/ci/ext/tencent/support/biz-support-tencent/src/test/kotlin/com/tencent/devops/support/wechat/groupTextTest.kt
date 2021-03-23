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

package com.tencent.devops.support.wechat

class groupTextTest {

//    @Ignore
//    @Test
//    fun groupText() {
//        val xmlString = """
//             <xml><ToUserName><![CDATA[wxab249edd27d57738]]></ToUserName><ServiceId><![CDATA[fw06b88b0e0531c52c]]></ServiceId><AgentType><![CDATA[chat]]></AgentType><Msg><From><Type><![CDATA[group]]></Type><Id><![CDATA[ww3008062624]]></Id><Sender><![CDATA[T01230005A]]></Sender><DeviceType><![CDATA[mac]]></DeviceType></From><CreateTime><![CDATA[1533129560]]></CreateTime><MsgType><![CDATA[text]]></MsgType><MsgId><![CDATA[CIKABBDY5obbBRjSt/XmhYCAAyCfAQ==]]></MsgId><Content><![CDATA[@DevOps(蓝盾助手) 会话id]]></Content><MentionedType>1</MentionedType></Msg></xml>
//            """
//        var document = DocumentHelper.parseText(xmlString)
//        var rootElement = document.rootElement
//        var toUserName = (rootElement.elementIterator("ToUserName").next() as Element).text
//        var serviceId = (rootElement.elementIterator("ServiceId").next() as Element).text
//        var agentType = (rootElement.elementIterator("AgentType").next() as Element).text
//        var msgElement = rootElement.elementIterator("Msg").next() as Element
//        var msgType = MsgType.valueOf((msgElement.elementIterator("MsgType").next() as Element).text.toUpperCase())
//        var fromElement = msgElement.elementIterator("From").next() as Element
//        var fromType = FromType.valueOf((fromElement.elementIterator("Type").next() as Element).text.toUpperCase())
//        var chatId = (fromElement.elementIterator("Id").next() as Element).text.toUpperCase()
//        var  callbackInfo = CallbackElement(
//                toUserName,
//                serviceId,
//                agentType,
//                chatId,
//                msgType,
//                fromType,
//                msgElement,
//                fromElement
//        )
//        val mentionType = (callbackInfo.msgElement.elementIterator("MentionedType").next() as Element).text
//        when (callbackInfo.msgType) {
//            MsgType.text -> {
//                val content = (callbackInfo.msgElement.elementIterator("Content").next() as Element).text
//                // 返回会话ID关键词
//                if(mentionType != "0" && content.contains("会话ID",true) ) {
//
//                    WechatWorkUtil.sendTextGroup("本会话的ID='$chatId'。PS:群ID可用于蓝盾平台上任意企业微信群通知。", chatId)
//
//                }
//
//            }
//            else -> { // 注意这个块
//            }
//        }
//    }
//
//    @Ignore
//    @Test
//    fun sendGroupText() {
//        var chatId = "ww3002055108"
//        WechatWorkUtil.sendTextGroup("本会话的ID='$chatId'。PS:群ID可用于蓝盾平台上任意企业微信群通知。", chatId)
//    }
}
