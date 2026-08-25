/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.pipeline.pojo

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "实例化-触发器配置")
data class TemplateInstanceTriggerConfig(
    @get:Schema(title = "插件stepId")
    val stepId: String? = null,
    @get:Schema(title = "启用或禁用")
    val disabled: Boolean? = null,

    val cron: List<String>? = null,
    @get:Schema(title = "触发器配置启动时启动的变量,目前仅定时触发支持")
    val variables: List<TemplateVariable>? = null
) {
    constructor(element: Element) : this(
        stepId = element.stepId,
        disabled = !element.elementEnabled(),
        cron = if (element is TimerTriggerElement) {
            element.advanceExpression
        } else {
            null
        },
        variables = if (element is TimerTriggerElement) {
            element.startParams?.let {
                JsonUtil.to(it, object : TypeReference<List<TemplateVariable>>() {})
            }
        } else {
            null
        }
    )
}
