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

package com.tencent.devops.stream.util

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_BUILD_TRIGGER
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_MANUAL_TRIGGER
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting

@Suppress("LongParameterList", "ReturnCount")
object StreamPipelineUtils {

    fun genStreamV2BuildUrl(
        homePage: String,
        gitProjectId: String,
        pipelineId: String,
        buildId: String,
        openCheckInId: String? = null,
        openCheckOutId: String? = null
    ): String {
        val url = "$homePage/pipeline/$pipelineId/detail/$buildId"
        if (!openCheckInId.isNullOrBlank()) {
            return url.plus("?checkIn=$openCheckInId#$gitProjectId")
        }
        if (!openCheckOutId.isNullOrBlank()) {
            return url.plus("?checkOut=$openCheckOutId#$gitProjectId")
        }
        return "$url/#$gitProjectId"
    }

    fun genStreamV2NotificationsUrl(
        streamUrl: String,
        gitProjectId: String,
        messageId: String
    ) = "$streamUrl/notifications?id=$messageId#$gitProjectId"

    fun createEmptyPipelineAndSetting(displayName: String) = PipelineModelAndSetting(
        model = Model(
            name = displayName,
            desc = "",
            stages = listOf(
                Stage(
                    id = VMUtils.genStageId(1),
                    name = VMUtils.genStageId(1),
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = I18nUtil.getCodeLanMessage(
                                messageCode = BK_BUILD_TRIGGER,
                                language = I18nUtil.getDefaultLocaleLanguage()
                            ),
                            elements = listOf(
                                ManualTriggerElement(
                                    name = I18nUtil.getCodeLanMessage(
                                        messageCode = BK_MANUAL_TRIGGER,
                                        language = I18nUtil.getDefaultLocaleLanguage()
                                    ),
                                    id = "T-1-1-1"
                                )
                            )
                        )
                    )
                )
            )
        ),
        setting = PipelineSetting(cleanVariablesWhenRetry = true)
    )
}
