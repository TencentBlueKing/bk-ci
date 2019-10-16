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
