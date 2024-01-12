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

import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.pojo.code.WebhookInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "构建详情-构建信息")
data class ModelRecord(
    @Schema(description = "构建ID", required = true)
    val id: String,
    @Schema(description = "流水线ID", required = true)
    val pipelineId: String,
    @Schema(description = "流水线名称", required = true)
    val pipelineName: String,
    @Schema(description = "启动用户", required = true)
    val userId: String,
    @Schema(description = "触发用户", required = true)
    val triggerUser: String? = null,
    @Schema(description = "触发条件", required = true)
    val trigger: String,
    @Schema(description = "触发时间（进队列时间）", required = true)
    val queueTime: Long,
    @Schema(description = "执行开始时间", required = true)
    val startTime: Long?,
    @Schema(description = "排队耗时（进队列到开始执行）", required = true)
    val queueTimeCost: Long?,
    @Schema(description = "执行结束时间", required = false)
    val endTime: Long?,
    @Schema(description = "Build status", required = true)
    val status: String,
    @Schema(description = "Build Model", required = true)
    val model: Model,
    @Schema(description = "服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @Schema(description = "构建号", required = true)
    val buildNum: Int,
    @Schema(description = "取消构建的用户", required = false)
    val cancelUserId: String?,
    @Schema(description = "本次执行的编排版本号", required = true)
    val curVersion: Int,
    @Schema(description = "流水线当前最新版本号", required = true)
    val latestVersion: Int,
    @Schema(description = "最新一次的构建buildNo", required = true)
    val latestBuildNum: Int,
    @Schema(description = "最近修改人", required = false)
    val lastModifyUser: String?,
    @Deprecated("保留是为了兼容detail，后续耗时不再以executeTime为准")
    @Schema(description = "执行耗时（排除系统耗时）流水线执行结束时才赋值", required = true)
    val executeTime: Long = 0,
    @Schema(description = "流水线任务执行错误", required = false)
    var errorInfoList: List<ErrorInfo>?,
    @Schema(description = "已执行stage的状态", required = false)
    var stageStatus: List<BuildStageStatus>?,
    @Schema(description = "触发审核人列表", required = false)
    val triggerReviewers: List<String>? = null,
    @Schema(description = "当前查询的执行次数", required = false)
    val executeCount: Int,
    @Deprecated("信息已在recordList中存在")
    @Schema(description = "历史重试执行人列表（有序）", required = true)
    val startUserList: List<String>,
    @Schema(description = "历史重试人列表（有序）", required = true)
    val recordList: List<BuildRecordInfo>,
    @Schema(description = "构建信息", required = false)
    var buildMsg: String?,
    @Schema(description = "原材料", required = false)
    val material: List<PipelineBuildMaterial>?,
    @Schema(description = "备注", required = false)
    val remark: String?,
    @Schema(description = "触发信息（包括代码库等）", required = false)
    val webhookInfo: WebhookInfo?,
    @Schema(description = "约束模式下的模板信息", required = false)
    var templateInfo: TemplateInfo? = null
)
