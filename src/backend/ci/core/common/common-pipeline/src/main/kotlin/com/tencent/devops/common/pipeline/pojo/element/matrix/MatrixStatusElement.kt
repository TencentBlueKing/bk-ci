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

package com.tencent.devops.common.pipeline.pojo.element.matrix

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-矩阵纯运行状态插件", description = MatrixStatusElement.classType)
data class MatrixStatusElement(
    @get:Schema(title = "任务名称", required = true)
    override var name: String = "状态插件",
    @get:Schema(title = "插件ID", required = false)
    override var id: String? = null,
    @get:Schema(title = "执行状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "执行次数", required = false)
    override var executeCount: Int = 1,
    @get:Schema(title = "执行时间", required = false)
    override var elapsed: Long? = null,
    @get:Schema(title = "启动时间", required = false)
    override var startEpoch: Long? = null,
    @get:Schema(title = "上下文标识", required = false)
    override var stepId: String?,
    @get:Schema(title = "用户自定义环境变量（插件运行时写入环境）", required = false)
    override var customEnv: List<NameAndValue>? = null,
    @get:Schema(title = "原插件的类型标识")
    val originClassType: String,
    @get:Schema(title = "原插件的市场标识")
    val originAtomCode: String?,
    @get:Schema(title = "原插件的内置标识")
    val originTaskAtom: String?,
    // 当状态插件为质量红线插件是需要专门保存
    @get:Schema(title = "审核人", required = true)
    var reviewUsers: MutableList<String>? = null,
    @get:Schema(title = "拦截原子", required = false)
    var interceptTask: String? = null,
    @get:Schema(title = "拦截原子名称", required = false)
    var interceptTaskName: String? = null,
    @get:Schema(title = "描述(人工审核插件使用)", required = false)
    var desc: String? = null,
    @get:Schema(title = "参数列表(人工审核插件使用)", required = false)
    var params: MutableList<ManualReviewParam>? = null
) : Element(
    name = name,
    status = status,
    executeCount = executeCount,
    elapsed = elapsed,
    startEpoch = startEpoch
) {

    companion object {
        const val classType = "matrixStatus"
    }

    override fun getClassType() = classType

    override fun getTaskAtom() = originTaskAtom ?: ""

    override fun getAtomCode() = originAtomCode ?: ""
}
