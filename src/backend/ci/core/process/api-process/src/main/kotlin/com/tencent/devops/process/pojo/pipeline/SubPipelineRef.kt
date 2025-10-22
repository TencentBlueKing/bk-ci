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

package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.EmptyElement
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "子流水线依赖信息")
data class SubPipelineRef(
    @get:Schema(title = "流水线Id", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(title = "流水线项目Id", required = true)
    val projectId: String,
    @get:Schema(title = "流水线项目渠道", required = true)
    val channel: String,
    @get:Schema(title = "插件", required = true)
    val element: Element,
    @get:Schema(title = "插件所在位置[stageIndex-containerIndex-taskIndex]", required = true)
    val taskPosition: String,
    @get:Schema(title = "子流水线流水线Id", required = true)
    val subPipelineId: String,
    @get:Schema(title = "子流水线项目Id", required = true)
    val subProjectId: String,
    @get:Schema(title = "子流水线名称", required = true)
    val subPipelineName: String,
    @get:Schema(title = "校验权限用户", required = true)
    val userId: String = "",
    @get:Schema(title = "插件启用状态", required = true)
    val elementEnable: Boolean = true,
    @get:Schema(title = "是否为模板流水线", required = true)
    val isTemplate: Boolean = false,
    @get:Schema(title = "插件参数[projectId]", required = false)
    val taskProjectId: String = "",
    @get:Schema(title = "插件参数[type]", required = false)
    val taskPipelineType: SubPipelineType = SubPipelineType.ID,
    @get:Schema(title = "插件参数[pipelineId]", required = false)
    val taskPipelineId: String? = "",
    @get:Schema(title = "插件参数[pipelineName]", required = false)
    val taskPipelineName: String? = ""
) {
    constructor(projectId: String, pipelineId: String, subPipelineId: String, subProjectId: String) : this(
        pipelineId = pipelineId,
        pipelineName = "",
        projectId = projectId,
        channel = "",
        element = EmptyElement(),
        subPipelineId = subPipelineId,
        subProjectId = subProjectId,
        subPipelineName = "",
        userId = "",
        elementEnable = true,
        isTemplate = false,
        taskPosition = ""
    )

    // 递归检查使用
    fun refKey() = "$projectId|$pipelineId"

    fun subRefKey() = "$subProjectId|$subPipelineId"

    // 链路打印
    fun chainKey() = "$projectId|$pipelineId|$taskPosition"
}
