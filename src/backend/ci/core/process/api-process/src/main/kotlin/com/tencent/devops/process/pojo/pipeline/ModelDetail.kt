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

import com.tencent.devops.common.pipeline.Model
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建详情-构建信息")
data class ModelDetail(
    @get:Schema(title = "构建ID", required = true)
    val id: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(title = "启动用户", required = true)
    val userId: String,
    @get:Schema(title = "触发用户", required = true)
    val triggerUser: String? = null,
    @get:Schema(title = "触发条件", required = true)
    val trigger: String,
    @get:Schema(title = "Start time", required = true)
    val startTime: Long,
    @get:Schema(title = "End time", required = false)
    val endTime: Long?,
    @get:Schema(title = "Build status", required = true)
    val status: String,
    @get:Schema(title = "Build Model", required = true)
    val model: Model,
    @get:Schema(title = "服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @get:Schema(title = "构建号", required = true)
    val buildNum: Int,
    @get:Schema(title = "取消构建的用户", required = false)
    val cancelUserId: String?,
    @get:Schema(title = "本次执行的编排版本号", required = true)
    val curVersion: Int,
    @get:Schema(title = "流水线当前最新版本号", required = true)
    val latestVersion: Int,
    @get:Schema(title = "最新一次的构建buildNo", required = true)
    val latestBuildNum: Int,
    @get:Schema(title = "最近修改人", required = true)
    val lastModifyUser: String?,
    @get:Schema(title = "执行耗时（排除系统耗时）流水线执行结束时才赋值", required = true)
    val executeTime: Long = 0,
    @get:Schema(title = "触发审核人列表", required = false)
    val triggerReviewers: List<String>? = null,
    @get:Schema(title = "是否为调试构建", required = false)
    val debug: Boolean? = false
)
