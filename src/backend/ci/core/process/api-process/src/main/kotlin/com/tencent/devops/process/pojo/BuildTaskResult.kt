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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-构建任务结果")
data class BuildTaskResult(
    @get:Schema(title = "任务ID", required = true)
    val taskId: String,
    @get:Schema(title = "插件ID", required = true)
    val elementId: String,
    @get:Schema(title = "插件版本号", required = false)
    val elementVersion: String? = null,
    @get:Schema(title = "容器Hash ID", required = true)
    val containerId: String?,
    @get:Schema(title = "是否执行成功", required = true)
    val success: Boolean,
    @get:Schema(title = "插件执行次数", required = true)
    val executeCount: Int? = null,
    @get:Schema(title = "构建结果", required = true)
    val buildResult: Map<String, String>,
    @get:Schema(title = "错误原因", required = false)
    val message: String? = null,
    @get:Schema(title = "任务类型", required = false)
    val type: String? = null,
    @get:Schema(title = "错误类型", required = false)
    val errorType: String? = null,
    @get:Schema(title = "错误码标识", required = false)
    val errorCode: Int? = null,
    @get:Schema(title = "对接平台代码", required = false)
    val platformCode: String? = null,
    @get:Schema(title = "对接平台错误码", required = false)
    val platformErrorCode: Int? = null,
    @get:Schema(title = "插件监控数据", required = false)
    val monitorData: Map<String, Any>? = null
)
