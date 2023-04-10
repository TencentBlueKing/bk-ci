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

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_HAS_BEEN_UPDATED
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_LATEST_EXPERIENCE_VERSION_CLICK_VIEW
import com.tencent.devops.experience.pojo.AppNotifyMessage

@SuppressWarnings("LongParameterList")
object AppNotifyUtil {
    /**
     * 标题：“【${应用名}】 ${版本号} 更新啦”。
     * 内容：“【${应用名}】发布了最新体验版本，蓝盾App诚邀您参与体验。点击查看>>”
     */
    fun makeMessage(
        experienceName: String,
        appVersion: String,
        experienceHashId: String,
        receiver: String,
        platform: String
    ): AppNotifyMessage {
        val message = AppNotifyMessage()
        message.receiver = receiver
        message.title =  MessageUtil.getMessageByLocale(
                messageCode = BK_HAS_BEEN_UPDATED,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(experienceName, appVersion)
            )
        message.body = MessageUtil.getMessageByLocale(
            messageCode = BK_LATEST_EXPERIENCE_VERSION_CLICK_VIEW,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
            params = arrayOf(experienceName)
        )
        message.url = "bkdevopsapp://bkdevopsapp/app/experience/expDetail/$experienceHashId"
        message.experienceHashId = experienceHashId
        message.platform = platform
        return message
    }
}
