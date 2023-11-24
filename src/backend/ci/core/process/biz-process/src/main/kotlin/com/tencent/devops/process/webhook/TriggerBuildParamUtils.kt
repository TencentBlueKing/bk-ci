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
 *
 */

package com.tencent.devops.process.webhook

import com.tencent.devops.common.pipeline.pojo.BuildEnvParameters
import com.tencent.devops.common.pipeline.pojo.BuildParameterGroup
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.web.utils.I18nUtil

object TriggerBuildParamUtils {
    // map<atomCode, map<event_type, params>>
    private val TRIGGER_BUILD_PARAM_NAME_MAP = mutableMapOf<String, Map<String, List<String>>>()
    private const val TRIGGER_BUILD_PARAM_PREFIX = "trigger.build.param"
    private const val TRIGGER_BUILD_PARAM_DESC = "desc"

    init {
        gitWebhookTriggerCommon()
        gitWebhookTriggerPush()
    }

    fun getTriggerParamNameMap(atomCode: String): List<BuildParameterGroup> {
        val paramNameMap = TRIGGER_BUILD_PARAM_NAME_MAP[atomCode] ?: emptyMap()
        return paramNameMap.map { (eventType, paramNames) ->
            val params = paramNames.map { paramName ->
                BuildEnvParameters(
                    name = paramName,
                    desc = I18nUtil.getCodeLanMessage(
                        messageCode = "$TRIGGER_BUILD_PARAM_PREFIX.$atomCode.$paramName.$TRIGGER_BUILD_PARAM_DESC"
                    )
                )
            }
            BuildParameterGroup(
                name = I18nUtil.getCodeLanMessage(messageCode = "$TRIGGER_BUILD_PARAM_PREFIX.$atomCode.$eventType"),
                params = params
            )
        }
    }

    /**
     * git事件触发公共变量名列表
     */
    private fun gitWebhookTriggerCommon() {
        val commonParams = listOf(
            "ci.repo_type",
            "ci.repo_url"
        )

        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType] = mapOf("common" to commonParams)
    }

    /**
     * git事件触发push变量名列表
     */
    private fun gitWebhookTriggerPush() {
        val pushParams = listOf(
            "ci.actor"
        )
        TRIGGER_BUILD_PARAM_NAME_MAP[CodeGitWebHookTriggerElement.classType] =
            mapOf(CodeEventType.PUSH.name to pushParams)
    }
}
