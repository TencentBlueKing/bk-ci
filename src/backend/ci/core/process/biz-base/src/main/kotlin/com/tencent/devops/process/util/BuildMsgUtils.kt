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

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_MANUAL_TRIGGER
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.StartType.MANUAL
import com.tencent.devops.common.pipeline.enums.StartType.PIPELINE
import com.tencent.devops.common.pipeline.enums.StartType.REMOTE
import com.tencent.devops.common.pipeline.enums.StartType.SERVICE
import com.tencent.devops.common.pipeline.enums.StartType.TIME_TRIGGER
import com.tencent.devops.common.pipeline.enums.StartType.WEB_HOOK
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_TRIGGER
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_PIPELINE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_REMOTE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_SERVICE
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_MSG_TIME

object BuildMsgUtils {

    private fun getDefaultValue(startType: StartType, channelCode: ChannelCode?): String {
        return when (startType) {
            MANUAL ->
                I18nUtil.getCodeLanMessage(messageCode = BK_MANUAL_TRIGGER)
            TIME_TRIGGER ->
                I18nUtil.getCodeLanMessage(messageCode = BUILD_MSG_TIME)
            REMOTE ->
                I18nUtil.getCodeLanMessage(messageCode = BUILD_MSG_REMOTE)
            SERVICE ->
                if (channelCode != null) {
                    val trigger = I18nUtil.getCodeLanMessage(BK_TRIGGER)
                    if (channelCode == ChannelCode.BS) {
                        "OpenAPI $trigger"
                    } else {
                        channelCode.name + trigger
                    }
                } else {
                    I18nUtil.getCodeLanMessage(messageCode = BUILD_MSG_SERVICE)
                }
            PIPELINE ->
                I18nUtil.getCodeLanMessage(messageCode = BUILD_MSG_PIPELINE)
            WEB_HOOK -> TODO()
        }
    }

    fun getBuildMsg(buildMsg: String?, startType: StartType, channelCode: ChannelCode?): String {
        return if (buildMsg.isNullOrBlank() || startType != WEB_HOOK) {
            getDefaultValue(startType = startType, channelCode = channelCode)
        } else {
            buildMsg
        }
    }
}
