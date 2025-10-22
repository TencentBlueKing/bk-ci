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

package com.tencent.devops.process.pojo.app.pipeline

import com.tencent.devops.common.pipeline.enums.StartType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "手机侧流水线构建历史模型")
data class AppPipelineHistory(
    @get:Schema(title = "项目id", required = false)
    val projectId: String,
    @get:Schema(title = "流水线id", required = false)
    val pipelineId: String,
    @get:Schema(title = "构建号id", required = false)
    val buildId: String,
    @get:Schema(title = "用户id", required = false)
    val userId: String,
    @get:Schema(title = "启动构建类型", required = false)
    val trigger: StartType,
    @get:Schema(title = "构建次数", required = false)
    val buildNum: Int?,
    @get:Schema(title = "启动时间", required = false)
    val startTime: Long?,
    @get:Schema(title = "结束时间", required = false)
    val endTime: Long?,
    @get:Schema(title = "状态", required = false)
    val status: String,
    @get:Schema(title = "当前服务器时间戳", required = false)
    val curTimestamp: Long,
    @get:Schema(title = "流水线版本", required = false)
    val pipelineVersion: Int,
    @get:Schema(title = "文件数量", required = false)
    var fileCount: Int = 0,
    @get:Schema(title = "所有文件总大小", required = false)
    var allFileSize: Long = 0,
    @get:Schema(title = "包版本", required = false)
    val packageVersion: String?,
    @get:Schema(title = "是否使用移动端构建", required = false)
    var isMobileStart: Boolean = false
)
