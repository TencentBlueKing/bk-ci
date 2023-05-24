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

package com.tencent.devops.experience.util

import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_LATEST_EXPERIENCE_VERSION_SHARING
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_LATEST_INVITES_YOU_EXPERIENCE
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_MOBILE_EXPERIENCE_ADDRESS
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_PC_EXPERIENCE_ADDRESS

object WechatGroupUtil {
    fun makeRichtextMessage(projectName: String, name: String, version: String, innerUrl: String, outerUrl: String, groupId: String): RichtextMessage {
        val receiver = Receiver(ReceiverType.group, groupId)
        val richtextContentList = mutableListOf<RichtextContent>()

        // title
        richtextContentList.add(
            RichtextText(
                RichtextTextText(
                    I18nUtil.getCodeLanMessage(
                        messageCode = BK_LATEST_EXPERIENCE_VERSION_SHARING,
                        params = arrayOf(projectName)
                    ) + "\n\n"
        )
            )
        )
        // body
        richtextContentList.add(RichtextText(RichtextTextText(
            I18nUtil.getCodeLanMessage(
                messageCode = BK_LATEST_INVITES_YOU_EXPERIENCE,
                params = arrayOf(projectName, name, version)
            )
        )))
        richtextContentList.add(
            RichtextView(
                RichtextViewLink(
                    I18nUtil.getCodeLanMessage(
                    messageCode = BK_PC_EXPERIENCE_ADDRESS
                ),
                innerUrl,
                1
        )
            )
        )
        richtextContentList.add(
            RichtextView(
                RichtextViewLink(
                    I18nUtil.getCodeLanMessage(messageCode = BK_MOBILE_EXPERIENCE_ADDRESS),
                    outerUrl,
                    1
                )
            )
        )
        return RichtextMessage(receiver, richtextContentList)
    }
}
