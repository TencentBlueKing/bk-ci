package com.tencent.devops.experience.util

import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink

object WechatGroupUtil {
    fun makeRichtextMessage(projectName: String, name: String, version: String, innerUrl: String, outerUrl: String, groupId: String): RichtextMessage {
        val receiver = Receiver(ReceiverType.group, groupId)
        val richtextContentList = mutableListOf<RichtextContent>()

        // title
        richtextContentList.add(
            RichtextText(
                RichtextTextText(
                "$projectName】最新体验版本分享\n\n"
        )
            )
        )
        // body
        richtextContentList.add(RichtextText(RichtextTextText(
                "【$projectName】发布了最新体验版本，【$name-$version】诚邀您参与体验。"
        )))
        richtextContentList.add(
            RichtextView(
                RichtextViewLink(
                "\nPC体验地址",
                innerUrl,
                1
        )
            )
        )
        richtextContentList.add(RichtextView(RichtextViewLink(
                "\n手机体验地址",
                outerUrl,
                1
        )))

        return RichtextMessage(receiver, richtextContentList)
    }
}