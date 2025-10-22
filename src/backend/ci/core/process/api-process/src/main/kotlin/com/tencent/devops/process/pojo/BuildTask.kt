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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-构建任务")
data class BuildTask(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "构建环境ID", required = true)
    val vmSeqId: String,
    @get:Schema(title = "任务状态", required = true)
    val status: BuildTaskStatus,
    @get:Schema(title = "插件执行次数", required = false)
    val executeCount: Int? = null,
    @get:Schema(title = "任务ID", required = true)
    val taskId: String? = null,
    @Deprecated("原本用于标识上下文但统一传了taskId，现废弃")
    @get:Schema(title = "插件ID", required = true)
    val elementId: String? = null,
    @get:Schema(title = "标识上下文的插件ID", required = true)
    val stepId: String? = null,
    @get:Schema(title = "插件名字", required = true)
    val elementName: String? = null,
    @get:Schema(title = "插件版本号", required = false)
    var elementVersion: String? = null,
    @get:Schema(title = "任务类型", required = false)
    val type: String? = null,
    @get:Schema(title = "任务参数", required = false)
    val params: Map<String, String>? = null,
    @get:Schema(title = "环境参数", required = false)
    var buildVariable: Map<String, String>? = null,
    @get:Schema(title = "容器类型", required = false)
    val containerType: String? = null,
    @get:Schema(title = "签名token", required = false)
    val signToken: String? = null
) {

    /**
     * 1、防止打印日志过大对象，影响IO
     * 2、防止敏感信息打印到日志
     */
    override fun toString() = "buildId=$buildId|vmSeqId=$vmSeqId|status=$status|taskId=$taskId|name=$elementName" +
        "stepId=$stepId|type=$type|paramSize=${params?.size}|buildVarSize=${buildVariable?.size}"
}
