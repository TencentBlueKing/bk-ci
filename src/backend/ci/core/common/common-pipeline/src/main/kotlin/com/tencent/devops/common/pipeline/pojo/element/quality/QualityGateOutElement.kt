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

package com.tencent.devops.common.pipeline.pojo.element.quality

import com.tencent.devops.common.api.constant.KEY_ELEMENT_ENABLE
import com.tencent.devops.common.api.constant.KEY_TASK_ATOM
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "质量红线(准出)", description = QualityGateOutElement.classType)
data class QualityGateOutElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "质量红线(准出)",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "拦截原子", required = false)
    var interceptTask: String? = null,
    @get:Schema(title = "拦截原子名称", required = false)
    var interceptTaskName: String? = null,
    @get:Schema(title = "审核人", required = false)
    var reviewUsers: Set<String>? = null
) : Element(name, id, status) {
    companion object {
        const val classType = "qualityGateOutTask"
    }

    override fun getTaskAtom() = "qualityGateOutTaskAtom"

    override fun getClassType() = classType

    override fun initTaskVar(): MutableMap<String, Any> {
        val taskVar = mutableMapOf<String, Any>()
        taskVar[QualityGateOutElement::name.name] = name
        taskVar[QualityGateOutElement::version.name] = version
        taskVar[KEY_TASK_ATOM] = getTaskAtom()
        taskVar[QualityGateInElement::classType.name] = getClassType()
        taskVar[KEY_ELEMENT_ENABLE] = elementEnabled()
        interceptTask?.let {
            taskVar[QualityGateOutElement::interceptTask.name] = it
        }
        interceptTaskName?.let {
            taskVar[QualityGateOutElement::interceptTaskName.name] = it
        }
        reviewUsers?.let {
            taskVar[QualityGateOutElement::reviewUsers.name] = it
        }
        retryCountAuto?.let { taskVar[::retryCountAuto.name] = it }
        return taskVar
    }
}
