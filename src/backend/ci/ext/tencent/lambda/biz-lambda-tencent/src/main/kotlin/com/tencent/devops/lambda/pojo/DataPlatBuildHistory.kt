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

package com.tencent.devops.lambda.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("历史构建模型")
data class DataPlatBuildHistory(
    @ApiModelProperty("清洗时间", required = false)
    val washTime: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("模板ID", required = true)
    val templateId: String,
    @ApiModelProperty("事业群名称", required = true)
    val bgName: String,
    @ApiModelProperty("部门名称", required = true)
    val deptName: String,
    @ApiModelProperty("中心名称", required = true)
    val centerName: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("启动用户", required = true)
    val userId: String,
    @ApiModelProperty("触发条件", required = true)
    val trigger: String,
    @ApiModelProperty("构建号", required = true)
    val buildNum: Int?,
    @ApiModelProperty("编排文件版本号", required = true)
    val pipelineVersion: Int,
    @ApiModelProperty("开始时间", required = true)
    val startTime: String,
    @ApiModelProperty("结束时间", required = true)
    val endTime: String?,
    @ApiModelProperty("状态", required = true)
    val status: String,
    @ApiModelProperty("状态枚举值", required = true)
    val statusOrdinal: Int,
    @ApiModelProperty("各阶段状态", required = true)
    val stageStatus: String?,
    @ApiModelProperty("结束原因", required = true)
    val deleteReason: String?,
    @ApiModelProperty("服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @ApiModelProperty("是否是手机启动", required = false)
    val isMobileStart: Boolean = false,
    @ApiModelProperty("原材料", required = false)
    val material: String?,
    @ApiModelProperty("排队于", required = false)
    val queueTime: Long?,
    @ApiModelProperty("构件列表", required = false)
    val artifactList: String?,
    @ApiModelProperty("备注", required = false)
    val remark: String?,
    @ApiModelProperty("总耗时(秒)", required = false)
    val totalTime: Long?,
    @ApiModelProperty("运行耗时(秒，不包括人工审核时间)", required = false)
    val executeTime: Long?,
    @ApiModelProperty("启动参数", required = false)
    val buildParameters: String?,
    @ApiModelProperty("WebHookType", required = false)
    val webHookType: String?,
    @ApiModelProperty("webhookInfo", required = false)
    val webhookInfo: String?,
    @ApiModelProperty("启动类型(新)", required = false)
    val startType: String?,
    @ApiModelProperty("推荐版本号", required = false)
    val recommendVersion: String?,
    @ApiModelProperty("是否重试", required = false)
    val retry: Boolean = false,
    @ApiModelProperty("流水线任务执行错误", required = false)
    var errorInfoList: String?,
    @ApiModelProperty("启动用户", required = false)
    var startUser: String?,
    @ApiModelProperty("渠道", required = false)
    var channel: String?,
    @ApiModelProperty("流水线标签", required = false)
    var labels: List<String>?,
    @ApiModelProperty("是否保密项目", required = false)
    val isSecrecy: Boolean?,
    @ApiModelProperty("构建描述", required = false)
    val buildMsg: String?,
    @ApiModelProperty("事业群ID", required = false)
    val bgId: String,
    @ApiModelProperty("部门ID", required = false)
    val deptId: String,
    @ApiModelProperty("中心ID", required = false)
    val centerId: String,
    @ApiModelProperty("非法变量名列表", required = false)
    val invalidKeyList: List<String>?
)
