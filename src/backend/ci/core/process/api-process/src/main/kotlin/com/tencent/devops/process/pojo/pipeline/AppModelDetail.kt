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

package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "APP-构建详情-构建信息")
data class AppModelDetail(
    @Schema(name = "ID", required = true)
    val buildId: String,
    @Schema(name = "启动用户", required = true)
    val userId: String,
    @Schema(name = "触发条件", required = true)
    val trigger: String,
    @Schema(name = "Start time", required = true)
    val startTime: Long,
    @Schema(name = "End time", required = false)
    val endTime: Long?,
    @Schema(name = "Build status", required = true)
    val status: String,
    @Schema(name = "服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @Schema(name = "构建号", required = true)
    val buildNum: Int,
    @Schema(name = "取消构建的用户", required = false)
    val cancelUserId: String?,
    @Schema(name = "归档文件个数", required = false)
    val fileCount: Int,
    @Schema(name = "包的版本(多个分号分隔)", required = false)
    val packageVersion: String,
    @Schema(name = "流水线Id", required = false)
    val pipelineId: String,
    @Schema(name = "流水线版本", required = false)
    val pipelineVersion: Int,
    @Schema(name = "流水线名字", required = false)
    var pipelineName: String,
    @Schema(name = "项目Id", required = false)
    val projectId: String,
    @Schema(name = "是否收藏", required = false)
    val hasCollect: Boolean,
    @Schema(name = "编排文件", required = true)
    val model: Model,
    @Schema(name = "原材料", required = false)
    val material: List<PipelineBuildMaterial>? = null,
    @Schema(name = "备注", required = false)
    val remark: String? = null,
    @Schema(name = "运行耗时(毫秒，不包括人工审核时间)", required = false)
    val executeTime: Long? = null,
    @Schema(name = "构建信息", required = false)
    var buildMsg: String? = null
)
