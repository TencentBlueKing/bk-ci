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

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.archive.pojo.ArtifactQualityMetadataAnalytics
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.pojo.code.WebhookInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "历史构建模型")
data class BuildHistory(
    @get:Schema(title = "构建ID", required = true)
    val id: String,
    @get:Schema(title = "启动用户", required = true)
    val userId: String,
    @get:Schema(title = "触发条件", required = true)
    val trigger: String,
    @get:Schema(title = "构建号", required = true)
    val buildNum: Int?,
    @get:Schema(title = "编排版本号", required = true)
    val pipelineVersion: Int,
    @get:Schema(title = "编排版本名称", required = true)
    val pipelineVersionName: String?,
    @get:Schema(title = "流水线的执行开始时间", required = true)
    val startTime: Long,
    @get:Schema(title = "流水线的执行结束时间", required = true)
    val endTime: Long?,
    @get:Schema(title = "状态", required = true)
    val status: String,
    @get:Schema(title = "各阶段状态", required = true)
    val stageStatus: List<BuildStageStatus>?,
    @get:Schema(title = "服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @get:Schema(title = "是否是手机启动", required = false)
    val isMobileStart: Boolean = false,
    @get:Schema(title = "原材料", required = false)
    val material: List<PipelineBuildMaterial>?,
    @get:Schema(title = "排队于（毫秒时间戳）", required = false)
    val queueTime: Long?,
    @get:Schema(title = "构件列表", required = false)
    val artifactList: List<FileInfo>?,
    @get:Schema(title = "制品质量分析", required = false)
    val artifactQuality: Map<String, List<ArtifactQualityMetadataAnalytics>>? = null,
    @get:Schema(title = "备注", required = false)
    val remark: String?,
    @get:Schema(title = "总耗时(毫秒)", required = false)
    val totalTime: Long?,
    @get:Schema(title = "运行耗时(毫秒，不包括人工审核时间)", required = false)
    val executeTime: Long?,
    @get:Schema(title = "启动参数", required = false)
    val buildParameters: List<BuildParameters>?,
    @get:Schema(title = "WebHookType", required = false)
    val webHookType: String?,
    @get:Schema(title = "webhookInfo", required = false)
    val webhookInfo: WebhookInfo?,
    @get:Schema(title = "启动类型(新)", required = false)
    val startType: String?,
    @get:Schema(title = "推荐版本号", required = false)
    val recommendVersion: String?,
    @get:Schema(title = "是否重试", required = false)
    val retry: Boolean = false,
    @get:Schema(title = "流水线任务执行错误", required = false)
    var errorInfoList: List<ErrorInfo>?,
    @get:Schema(title = "构建信息", required = false)
    var buildMsg: String?,
    @get:Schema(title = "自定义构建版本号", required = false)
    val buildNumAlias: String? = null,
    @get:Schema(title = "流水线编排的最后更新时间", required = false)
    val updateTime: Long? = null,
    @get:Schema(title = "并发时,设定的group", required = false)
    var concurrencyGroup: String? = null,
    @get:Schema(title = "构建执行次数（重试次数-1）", required = false)
    val executeCount: Int?
)
