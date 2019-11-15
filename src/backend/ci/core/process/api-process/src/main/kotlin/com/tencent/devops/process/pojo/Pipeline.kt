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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-列表信息")
data class Pipeline(
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    var pipelineName: String,
    @ApiModelProperty("流水线描述", required = false)
    var pipelineDesc: String?,
    @ApiModelProperty("流水线任务数量", required = true)
    val taskCount: Int,
    @ApiModelProperty("构建次数", required = true)
    val buildCount: Long,
    @ApiModelProperty("运行锁定", required = false)
    val lock: Boolean = false,
    @ApiModelProperty("是否可手工启动", required = true)
    val canManualStartup: Boolean,
    @ApiModelProperty("最后构建启动时间", required = false)
    val latestBuildStartTime: Long?,
    @ApiModelProperty("最后构建结束时间", required = false)
    val latestBuildEndTime: Long?,
    @ApiModelProperty("最后构建状态", required = false)
    var latestBuildStatus: BuildStatus?,
    @ApiModelProperty("最后构建版本号", required = false)
    val latestBuildNum: Int?,
    @ApiModelProperty("最后构建任务名称", required = false)
    val latestBuildTaskName: String?,
    @ApiModelProperty("最后任务预计执行时间（秒）", required = false)
    val latestBuildEstimatedExecutionSeconds: Long?,
    @ApiModelProperty("最后构建实例ID", required = false)
    val latestBuildId: String?,
    @ApiModelProperty("部署时间", required = true)
    val deploymentTime: Long,
    @ApiModelProperty("流水线创建时间", required = true)
    val createTime: Long,
    @ApiModelProperty("编排文件版本号", required = true)
    val pipelineVersion: Int,
    @ApiModelProperty("服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @ApiModelProperty("当前运行的构建的个数", required = true)
    val runningBuildCount: Int,
    @ApiModelProperty("是否有list权限", required = true)
    val hasPermission: Boolean,
    @ApiModelProperty("是否被收藏", required = true)
    val hasCollect: Boolean,
    @ApiModelProperty("最后执行人id", required = false)
    var latestBuildUserId: String = "",
    @ApiModelProperty("是否从模板中实例化出来的", required = false)
    val instanceFromTemplate: Boolean? = null
)
