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

package com.tencent.devops.process.pojo.pipeline.record

import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "构建详情记录-插件任务")
data class BuildRecordModel(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "编排版本号", required = true)
    val resourceVersion: Int,
    @get:Schema(title = "构建号", required = true)
    val buildNum: Int,
    @get:Schema(title = "执行次数", required = true)
    val executeCount: Int,
    @get:Schema(title = "执行变量", required = true)
    val modelVar: MutableMap<String, Any>,
    @get:Schema(title = "触发时间", required = false)
    val queueTime: LocalDateTime,
    @get:Schema(title = "触发人", required = true)
    val startUser: String,
    @get:Schema(title = "触发器", required = true)
    val startType: String,
    @get:Schema(title = "构建状态", required = false)
    var status: String? = null,
    @get:Schema(title = "取消人", required = false)
    val cancelUser: String? = null,
    @get:Schema(title = "开始时间", required = true)
    var startTime: LocalDateTime? = null,
    @get:Schema(title = "结束时间", required = true)
    var endTime: LocalDateTime? = null,
    @get:Schema(title = "流水线任务执行错误", required = false)
    var errorInfoList: List<ErrorInfo>? = null,
    @get:Schema(title = "业务时间戳集合", required = false)
    var timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>
)
