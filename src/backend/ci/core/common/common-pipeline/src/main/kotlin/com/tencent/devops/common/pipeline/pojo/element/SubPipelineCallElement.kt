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

package com.tencent.devops.common.pipeline.pojo.element

import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "子流水线调用", description = SubPipelineCallElement.classType)
data class SubPipelineCallElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "自流水线调用",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "子流水线ID", required = true)
    val subPipelineId: String = "",
    @get:Schema(title = "是否异步", required = true)
    val asynchronous: Boolean,
    @get:Schema(title = "新版的子流水线原子的类型")
    val subPipelineType: SubPipelineType? = SubPipelineType.ID,
    @get:Schema(title = "新版的子流水线名")
    val subPipelineName: String? = null,
    @get:Schema(title = "启动参数", required = false)
    val parameters: Map<String, String>?
) : Element(name, id, status) {
    companion object {
        const val classType = "subPipelineCall"
        const val TASK_ATOM = "subPipelineCallAtom"
    }
    override fun getTaskAtom() = TASK_ATOM
    override fun getClassType() = classType
}
