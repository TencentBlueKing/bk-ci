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

import com.tencent.devops.common.pipeline.event.PipelineCallbackEvent
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线编排解析后数据")
data class PipelineModelBasicInfo(
    @get:Schema(title = "是否能够手动启动")
    val canManualStartup: Boolean,
    @get:Schema(title = "是否可以跳过")
    val canElementSkip: Boolean,
    @get:Schema(title = "任务数")
    val taskCount: Int,
    @get:Schema(title = "参数")
    val param: List<BuildFormProperty>? = null,
    @get:Schema(title = "构建版本号", required = false)
    val buildNo: BuildNo? = null,
    @get:Schema(title = "流水线事件回调", required = false)
    val events: Map<String, PipelineCallbackEvent>? = emptyMap(),
    @get:Schema(title = "流水线插件", required = false)
    val modelTasks: List<PipelineModelTask> = emptyList(),
    @get:Schema(title = "静态流水线组", required = false)
    val staticViews: List<String> = emptyList()
)
