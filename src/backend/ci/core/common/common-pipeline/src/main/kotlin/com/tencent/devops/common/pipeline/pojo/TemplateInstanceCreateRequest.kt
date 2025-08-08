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

import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

data class TemplateInstanceCreateRequest(
    @get:Schema(title = "模板ID", required = true)
    var templateId: String,
    @get:Schema(title = "模板版本号（为空时默认最新）", required = true)
    var templateVersion: Long?,
    @get:Schema(title = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(title = "是否使用通知配置", required = false)
    var useSubscriptionSettings: Boolean?,
    @get:Schema(title = "是否使用标签配置", required = false)
    var useLabelSettings: Boolean?,
    @get:Schema(title = "是否使用并发组配置", required = false)
    var useConcurrencyGroup: Boolean?,
    @get:Schema(title = "创建实例的模式", required = false)
    var instanceType: String? = PipelineInstanceTypeEnum.FREEDOM.type,
    @get:Schema(title = "是否为空模板", required = false)
    var emptyTemplate: Boolean? = false,
    @get:Schema(title = "静态流水线组", required = false)
    var staticViews: List<String> = emptyList(),
    @get:Schema(title = "是否继承项目流水线语言风格", required = false)
    var inheritedDialect: Boolean? = true,
    @get:Schema(title = "流水线语言风格", required = false)
    var pipelineDialect: String? = null,
    @get:Schema(title = "流水线标签", required = false)
    val labels: List<String> = emptyList()
)
