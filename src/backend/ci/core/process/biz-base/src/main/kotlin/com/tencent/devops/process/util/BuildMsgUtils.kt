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

package com.tencent.devops.process.util

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_MANUAL
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_PIPELINE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_REMOTE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_SERVICE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_TIME
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_WEBHOOK

object BuildMsgUtils {

    private fun getDefaultValue(startType: StartType, channelCode: ChannelCode?): String {
        return when (startType) {
            StartType.MANUAL ->
                MessageUtil.getCodeLanMessage(
                    messageCode = BUILD_MSG_MANUAL,
                    defaultMessage = "手动触发",
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            StartType.TIME_TRIGGER ->
                MessageUtil.getCodeLanMessage(
                    messageCode = BUILD_MSG_TIME,
                    defaultMessage = "定时触发",
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            StartType.WEB_HOOK ->
                MessageUtil.getCodeLanMessage(
                    messageCode = BUILD_MSG_WEBHOOK,
                    defaultMessage = "webhook触发",
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            StartType.REMOTE ->
                MessageUtil.getCodeLanMessage(
                    messageCode = BUILD_MSG_REMOTE,
                    defaultMessage = "远程触发",
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            StartType.SERVICE ->
                if (channelCode != null) {
                    if (channelCode == ChannelCode.BS) {
                        "OpenAPI触发"
                    } else {
                        channelCode.name + "触发"
                    }
                } else {
                    MessageUtil.getCodeLanMessage(
                        messageCode = BUILD_MSG_SERVICE,
                        defaultMessage = "服务触发",
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    )
                }
            StartType.PIPELINE ->
                MessageUtil.getCodeLanMessage(
                    messageCode = BUILD_MSG_PIPELINE,
                    defaultMessage = "流水线调用触发",
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
        }
    }

    fun getBuildMsg(buildMsg: String?, startType: StartType, channelCode: ChannelCode?): String {
        return if (buildMsg.isNullOrBlank()) {
            getDefaultValue(startType = startType, channelCode = channelCode)
        } else {
            buildMsg
        }
    }
}
