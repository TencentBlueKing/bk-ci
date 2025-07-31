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

import com.tencent.devops.common.archive.pojo.ArtifactQualityMetadataAnalytics
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "APP-构建详情-构建信息")
data class AppModelDetail(
    @get:Schema(title = "ID", required = true)
    val buildId: String,
    @get:Schema(title = "启动用户", required = true)
    val userId: String,
    @get:Schema(title = "触发条件", required = true)
    val trigger: String,
    @get:Schema(title = "Start time", required = true)
    val startTime: Long,
    @get:Schema(title = "End time", required = false)
    val endTime: Long?,
    @get:Schema(title = "Build status", required = true)
    val status: String,
    @get:Schema(title = "服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @get:Schema(title = "构建号", required = true)
    val buildNum: Int,
    @get:Schema(title = "取消构建的用户", required = false)
    val cancelUserId: String?,
    @get:Schema(title = "归档文件个数", required = false)
    val fileCount: Int,
    @get:Schema(title = "包的版本(多个分号分隔)", required = false)
    val packageVersion: String,
    @get:Schema(title = "流水线Id", required = false)
    val pipelineId: String,
    @get:Schema(title = "流水线版本", required = false)
    val pipelineVersion: Int,
    @get:Schema(title = "流水线名字", required = false)
    var pipelineName: String,
    @get:Schema(title = "项目Id", required = false)
    val projectId: String,
    @get:Schema(title = "是否收藏", required = false)
    val hasCollect: Boolean,
    @get:Schema(title = "编排文件", required = true)
    val model: Model,
    @get:Schema(title = "原材料", required = false)
    val material: List<PipelineBuildMaterial>? = null,
    @get:Schema(title = "备注", required = false)
    val remark: String? = null,
    @get:Schema(title = "运行耗时(毫秒，不包括人工审核时间)", required = false)
    val executeTime: Long? = null,
    @get:Schema(title = "构建信息", required = false)
    var buildMsg: String? = null,
    @get:Schema(title = "制品质量分析", required = false)
    val artifactQuality: Map<String, List<ArtifactQualityMetadataAnalytics>>? = null
)
