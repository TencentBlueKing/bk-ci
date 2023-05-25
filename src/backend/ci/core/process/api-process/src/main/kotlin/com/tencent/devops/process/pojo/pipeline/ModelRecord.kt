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
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建详情-构建信息")
data class ModelRecord(
    @ApiModelProperty("构建ID", required = true)
    val id: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    val pipelineName: String,
    @ApiModelProperty("启动用户", required = true)
    val userId: String,
    @ApiModelProperty("触发用户", required = true)
    val triggerUser: String? = null,
    @ApiModelProperty("触发条件", required = true)
    val trigger: String,
    @ApiModelProperty("触发时间（进队列时间）", required = true)
    val queueTime: Long,
    @ApiModelProperty("执行开始时间", required = true)
    val startTime: Long?,
    @ApiModelProperty("排队耗时（进队列到开始执行）", required = true)
    val queueTimeCost: Long?,
    @ApiModelProperty("执行结束时间", required = false)
    val endTime: Long?,
    @ApiModelProperty("Build status", required = true)
    val status: String,
    @ApiModelProperty("Build Model", required = true)
    val model: Model,
    @ApiModelProperty("服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @ApiModelProperty("构建号", required = true)
    val buildNum: Int,
    @ApiModelProperty("取消构建的用户", required = false)
    val cancelUserId: String?,
    @ApiModelProperty("本次执行的编排版本号", required = true)
    val curVersion: Int,
    @ApiModelProperty("流水线当前最新版本号", required = true)
    val latestVersion: Int,
    @ApiModelProperty("最新一次的构建buildNo", required = true)
    val latestBuildNum: Int,
    @ApiModelProperty("最近修改人", required = false)
    val lastModifyUser: String?,
    @Deprecated("保留是为了兼容detail，后续耗时不再以executeTime为准")
    @ApiModelProperty("执行耗时（排除系统耗时）流水线执行结束时才赋值", required = true)
    val executeTime: Long = 0,
    @ApiModelProperty("流水线任务执行错误", required = false)
    var errorInfoList: List<ErrorInfo>?,
    @ApiModelProperty("已执行stage的状态", required = false)
    var stageStatus: List<BuildStageStatus>?,
    @ApiModelProperty("触发审核人列表", required = false)
    val triggerReviewers: List<String>? = null,
    @ApiModelProperty("当前查询的执行次数", required = false)
    val executeCount: Int,
    @ApiModelProperty("历史重试执行人列表（有序）", required = true)
    val startUserList: List<String>,
    @ApiModelProperty("构建信息", required = false)
    var buildMsg: String?,
    @ApiModelProperty("原材料", required = false)
    val material: List<PipelineBuildMaterial>?,
    @ApiModelProperty("备注", required = false)
    val remark: String?,
    @ApiModelProperty("触发信息（包括代码库等）", required = false)
    val webhookInfo: WebhookInfo?
)
