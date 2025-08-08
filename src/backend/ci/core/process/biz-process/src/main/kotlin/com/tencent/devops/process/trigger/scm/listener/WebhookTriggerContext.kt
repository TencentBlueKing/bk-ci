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

package com.tencent.devops.process.trigger.scm.listener

import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "webhook触发上下文")
data class WebhookTriggerContext(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "流水线ID/yaml文件名/.ci,如果yaml还没生成流水线,就是yaml文件名;在获取.ci文件树失败,就是.ci")
    val pipelineId: String,
    @get:Schema(title = "事件ID")
    val eventId: Long,
    @get:Schema(title = "构建ID")
    var buildId: BuildId? = null,
    @get:Schema(title = "流水线信息")
    var pipelineInfo: PipelineInfo? = null,
    @get:Schema(title = "启动参数")
    var startParams: Map<String, Any>? = null,
    @get:Schema(title = "失败匹配的插件")
    var failedMatchElements: List<PipelineTriggerFailedMatchElement>? = null
)
