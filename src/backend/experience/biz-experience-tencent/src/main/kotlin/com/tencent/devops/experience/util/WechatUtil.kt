package com.tencent.devops.experience.util

import com.tencent.devops.notify.pojo.WechatNotifyMessage

object WechatUtil {
    fun makeMessage(projectName: String, name: String, version: String, innerUrl: String, outerUrl: String, receivers: Set<String>): WechatNotifyMessage {
        val message = WechatNotifyMessage()
        message.addAllReceivers(receivers)
        message.body = "【$projectName】发布了最新体验版本，【$name-$version】诚邀您参与体验。\nPC体验地址：$innerUrl\n手机体验地址：$outerUrl"
        return message
    }
}