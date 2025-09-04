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
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_EXPERIENCE_ADD_GROUP_CONTENT
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_EXPERIENCE_ADD_GROUP_FOOTER
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_EXPERIENCE_ADD_GROUP_HEADER
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_EXPERIENCE_ADD_GROUP_TITLE
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_LATEST_EXPERIENCE_VERSION_INFO
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_LATEST_EXPERIENCE_VERSION_SHARING
import com.tencent.devops.notify.pojo.RtxNotifyMessage

@SuppressWarnings("LongParameterList")
object RtxUtil {
    fun makeMessage(
        projectName: String,
        name: String,
        version: String,
        appUrl: String,
        receivers: Set<String>
    ): RtxNotifyMessage {
        val message = RtxNotifyMessage()
        message.addAllReceivers(receivers)
        message.title = I18nUtil.getCodeLanMessage(
            messageCode = BK_LATEST_EXPERIENCE_VERSION_SHARING,
            params = arrayOf(projectName)
        )
        message.body = I18nUtil.getCodeLanMessage(
            messageCode = BK_LATEST_EXPERIENCE_VERSION_INFO,
            params = arrayOf(projectName, name, version, appUrl)
        )
        return message
    }

    fun batchLatestMessage(
        projectName: String,
        messages: List<Message>,
        receivers: Set<String>
    ): RtxNotifyMessage {
        val rtxNotifyMessage = RtxNotifyMessage()
        rtxNotifyMessage.addAllReceivers(receivers)
        rtxNotifyMessage.title = I18nUtil.getCodeLanMessage(
            messageCode = BK_LATEST_EXPERIENCE_VERSION_SHARING,
            params = arrayOf(projectName)
        )
        val body = StringBuilder()
        for (m in messages) {
            body.append(
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_LATEST_EXPERIENCE_VERSION_INFO,
                    params = arrayOf(projectName, m.name, m.version, m.outerUrl)
                ) + "\n\n"
            )
        }
        rtxNotifyMessage.body = body.toString()
        return rtxNotifyMessage
    }

    fun batchAddGroupMessage(
        receivers: Set<String>,
        groupName: String,
        masterId: String,
        messages: List<Message>,
        projectName: String
    ): RtxNotifyMessage {
        val rtxNotifyMessage = RtxNotifyMessage()
        rtxNotifyMessage.addAllReceivers(receivers)
        rtxNotifyMessage.title = I18nUtil.getCodeLanMessage(
            messageCode = BK_EXPERIENCE_ADD_GROUP_TITLE,
            params = arrayOf(projectName)
        )
        val body = StringBuilder()
        body.append(
            I18nUtil.getCodeLanMessage(
                messageCode = BK_EXPERIENCE_ADD_GROUP_HEADER,
                params = arrayOf(groupName, masterId, messages.size.toString())
            ) + "\n"
        )
        for (i in 0..1) {
            val m = messages[i]
            body.append(
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_EXPERIENCE_ADD_GROUP_CONTENT,
                    params = arrayOf((i + 1).toString(), m.name, m.version, m.outerUrl)
                ) + "\n"
            )
        }
        body.append(I18nUtil.getCodeLanMessage(messageCode = BK_EXPERIENCE_ADD_GROUP_FOOTER))
        rtxNotifyMessage.body = body.toString()
        return rtxNotifyMessage
    }
}
