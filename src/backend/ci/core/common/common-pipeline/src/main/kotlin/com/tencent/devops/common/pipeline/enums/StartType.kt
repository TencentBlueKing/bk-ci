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

package com.tencent.devops.common.pipeline.enums

import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import org.slf4j.LoggerFactory

enum class StartType {
    MANUAL,
    TIME_TRIGGER,
    WEB_HOOK,
    SERVICE,
    PIPELINE,
    REMOTE;

    companion object {
        fun toReadableString(type: String, channelCode: ChannelCode?, language: String): String {
            var params: Array<String>? = null
            val name = when (type) {
                StartType.MANUAL.name -> MANUAL.name
                StartType.TIME_TRIGGER.name -> TIME_TRIGGER.name
                StartType.WEB_HOOK.name -> WEB_HOOK.name
                StartType.REMOTE.name -> REMOTE.name
                StartType.SERVICE.name -> {
                    if (channelCode != null) {
                        if (channelCode == ChannelCode.BS) {
                            "${SERVICE.name}_${ChannelCode.BS}"
                        } else {
                            params = arrayOf(channelCode.name)
                            "${SERVICE.name}_CHANNEL"
                        }
                    } else {
                        "${SERVICE.name}_NOT_CHANNEL"
                    }
                }
                StartType.PIPELINE.name -> PIPELINE.name
                "" -> ""
                else -> type
            }
            return if (name.isBlank()) name else {
                MessageUtil.getMessageByLocale(
                    messageCode = "START_TYPE_$name",
                    language = language,
                    params = params
                )
            }
        }

        fun toStartType(type: String): StartType {
            values().forEach {
                if (type.equals(it.name, true)) {
                    return it
                }
            }
            logger.warn("Unknown start type($type)")
            return MANUAL
        }

        private val logger = LoggerFactory.getLogger(StartType::class.java)

        fun getStartTypeMap(language: String): List<IdValue> {
            val result = mutableListOf<IdValue>()
            values().forEach {
                result.add(IdValue(it.name, toReadableString(it.name, null, language)))
            }

            return result
        }

        fun transform(startType: String, webhookType: String?): String {
            return when (startType) {
                MANUAL.name -> ManualTriggerElement.classType
                TIME_TRIGGER.name -> TimerTriggerElement.classType
                WEB_HOOK.name -> {
                    when (webhookType) {
                        CodeType.SVN.name -> CodeSVNWebHookTriggerElement.classType
                        CodeType.GIT.name -> CodeGitWebHookTriggerElement.classType
                        CodeType.GITLAB.name -> CodeGitlabWebHookTriggerElement.classType
                        CodeType.GITHUB.name -> CodeGithubWebHookTriggerElement.classType
                        CodeType.TGIT.name -> CodeTGitWebHookTriggerElement.classType
                        else -> RemoteTriggerElement.classType
                    }
                }

                else -> { // SERVICE.name,  PIPELINE.name, REMOTE.name
                    RemoteTriggerElement.classType
                }
            }
        }
    }
}
