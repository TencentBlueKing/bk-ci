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

import com.tencent.devops.common.api.pojo.OS
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 流水线运行环境操作系统的变更记录。
 *
 * 部分渠道的运行环境由流水线设置指定而非编排自身指定(如创作流的创作环境)，
 * 这类渠道变更运行环境后，编排中的插件未必仍适用于新的操作系统，需要在保存时校验。
 * 该对象仅在「确实发生了操作系统变更」时才会被构造，其余场景为 null，
 * 使不具备该语义的渠道走原有逻辑、零额外开销。
 */
@Schema(title = "流水线运行环境操作系统变更记录")
data class PipelineRunEnvOsChange(
    @get:Schema(title = "变更前运行环境的操作系统", required = true)
    val previousOs: OS,
    @get:Schema(title = "变更后运行环境的操作系统", required = true)
    val currentOs: OS
)
