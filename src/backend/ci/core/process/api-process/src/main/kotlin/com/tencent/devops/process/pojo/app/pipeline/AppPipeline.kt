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

package com.tencent.devops.process.pojo.app.pipeline

import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("app流水线信息")
data class AppPipeline(
    @ApiModelProperty("项目id", required = false)
    val projectId: String,
    @ApiModelProperty("项目名称", required = false)
    var projectName: String,
    @ApiModelProperty("流水线id", required = false)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = false)
    var pipelineName: String,
    @ApiModelProperty("流水线描述", required = false)
    var pipelineDesc: String,
    @ApiModelProperty("最新构建状态", required = false)
    val latestBuildStatus: BuildStatus?,
    @ApiModelProperty("最近一次构建序号", required = false)
    val latestBuildNum: Int?,
    @ApiModelProperty("最新构建号id", required = false)
    val latestBuildId: String?,
    @ApiModelProperty("最近构建启动时间", required = false)
    val latestBuildStartTime: Long?,
    @ApiModelProperty("最近构建结束时间", required = false)
    val latestBuildEndTime: Long?,
    @ApiModelProperty("最近构建用户", required = false)
    var latestBuildUser: String,
    @ApiModelProperty("流水线版本", required = false)
    val pipelineVersion: Int,
    @ApiModelProperty("是否可手工启动", required = true)
    val canManualStartup: Boolean,
    var hasCollect: Boolean = false,
    @ApiModelProperty("部署时间", required = true)
    val deploymentTime: Long = 0L,
    @ApiModelProperty("流水线创建时间", required = true)
    val createTime: Long = 0L,
    @ApiModelProperty("项目图标链接", required = false)
    val logoUrl: String = ""
)
