package com.tencent.devops.experience.util

import com.tencent.devops.notify.pojo.RtxNotifyMessage

object RtxUtil {
    fun makeMessage(projectName: String, name: String, version: String, innerUrl: String, outerUrl: String, receivers: Set<String>): RtxNotifyMessage {
        val message = RtxNotifyMessage()
        message.addAllReceivers(receivers)
        message.title = "【$projectName】最新体验版本分享"
        message.body = "【$projectName】发布了最新体验版本，【$name-$version】诚邀您参与体验。\n<a  href=\"$innerUrl\">PC体验地址</a>\n<a href=\"$outerUrl\">手机体验地址</a>"
        return message
    }
}