package com.tencent.devops.support.model.wechatwork

import com.tencent.devops.support.model.wechatwork.enums.FromType
import com.tencent.devops.support.model.wechatwork.enums.MsgType
import org.dom4j.Element

data class CallbackElement(
        // 公共部分抽离出来
    val toUserName: String, // 接收人，一般只的都是我们wxab249edd27d57738的id
    val serviceId: String, // 服务号id,fw06b88b0e0531c52c
    val agentType: String, // 一般都是chat
    val chatId: String, // 发送人
    val msgType: MsgType, // xml.Msg.MsgType
    val fromType: FromType, // xml.Msg.MsgType
        // 没办法抽离出来的，讲Msg整个element存放起来
    val msgElement: Element, // xml.Msg
    val fromElement: Element // xml.Msg.From

)
