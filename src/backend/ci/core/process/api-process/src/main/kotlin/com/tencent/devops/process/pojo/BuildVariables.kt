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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.store.pojo.app.BuildEnv
import io.swagger.v3.oas.annotations.media.Schema
import java.util.concurrent.TimeUnit

@Schema(title = "流水线模型-构建参数变量")
data class BuildVariables(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "构建环境ID", required = true)
    val vmSeqId: String,
    @get:Schema(title = "构建机名称", required = true)
    val vmName: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "pipeline id", required = true)
    val pipelineId: String,
    @get:Schema(title = "参数集合（已完成上下文转换）", required = true)
    var variables: Map<String, String>,
    @get:Schema(title = "系统环境变量", required = false)
    val buildEnvs: List<BuildEnv>,
    @get:Schema(title = "container的编排ID（同seq）", required = false)
    val containerId: String,
    @get:Schema(title = "container的全局ID", required = false)
    val containerHashId: String,
    @get:Schema(title = "container用户自定义ID", required = false)
    val jobId: String?,
    @get:Schema(title = "参数类型集合（用于打印时区分敏感信息，建议不要作为传参使用）", required = false)
    var variablesWithType: List<BuildParameters>,
    @get:Schema(title = "Job超时时间（毫秒）", required = true)
    var timeoutMills: Long = TimeUnit.MINUTES.toMillis(Timeout.DEFAULT_TIMEOUT_MIN.toLong()),
    @get:Schema(title = "容器类型", required = false)
    val containerType: String? = null,
    @get:Schema(title = "YAML流水线特殊配置", required = false)
    val pipelineAsCodeSettings: PipelineAsCodeSettings? = null
)
