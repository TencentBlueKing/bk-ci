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

package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.pojo.TemplateInstanceField
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceTriggerConfig
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创建/升级单条实例请求体")
data class PipelineTemplateInstanceReleaseInfo(
    @get:Schema(title = "流水线ID，创建时可为空", required = false)
    val pipelineId: String = "",
    @get:Schema(title = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(title = "构建号（推荐版本号）", required = false)
    val buildNo: BuildNo?,
    @get:Schema(title = "流水线变量列表", required = false)
    val param: List<BuildFormProperty>? = null,
    @get:Schema(title = "触发器配置", required = false)
    val triggerConfigs: List<TemplateInstanceTriggerConfig>? = null,
    @get:Schema(title = "yaml文件路径", required = true)
    val filePath: String? = null,
    @get:Schema(title = "覆盖模版字段", required = false)
    val overrideTemplateField: TemplateInstanceField? = null,
    @get:Schema(title = "重置实例推荐版本为基准值", required = false)
    val resetBuildNo: Boolean? = false
)
