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

package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.pojo.classify.PipelineGroupLabels
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
    var pipelineDesc: String? = null,
    @ApiModelProperty("流水线任务数量", required = true)
    val taskCount: Int,
    @ApiModelProperty("构建次数", required = true)
    var buildCount: Long = 0,
    @ApiModelProperty("运行锁定", required = false)
    var lock: Boolean = false,
    @ApiModelProperty("是否可手工启动", required = true)
    val canManualStartup: Boolean,
    @ApiModelProperty("最后构建启动时间", required = false)
    var latestBuildStartTime: Long? = null,
    @ApiModelProperty("最后构建结束时间", required = false)
    var latestBuildEndTime: Long? = null,
    @ApiModelProperty("最后构建状态", required = false)
    var latestBuildStatus: BuildStatus? = null,
    @ApiModelProperty("最后构建版本号", required = false)
    var latestBuildNum: Int? = null,
    @ApiModelProperty("最后构建任务名称", required = false)
    var latestBuildTaskName: String? = null,
    @ApiModelProperty("最后任务预计执行时间（毫秒）", required = false)
    val latestBuildEstimatedExecutionSeconds: Long?,
    @ApiModelProperty("最后构建实例ID", required = false)
    var latestBuildId: String? = null,
    @ApiModelProperty("部署时间", required = true)
    val deploymentTime: Long,
    @ApiModelProperty("流水线创建时间", required = true)
    val createTime: Long = deploymentTime,
    @ApiModelProperty("更新时间", required = true)
    val updateTime: Long,
    @ApiModelProperty("编排文件版本号", required = true)
    val pipelineVersion: Int,
    @ApiModelProperty("服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @ApiModelProperty("当前运行的构建的个数", required = true)
    var runningBuildCount: Int = 0,
    @ApiModelProperty("是否有list权限", required = true)
    val hasPermission: Boolean,
    @ApiModelProperty("是否被收藏", required = true)
    val hasCollect: Boolean,
    @ApiModelProperty("最后执行人id", required = false)
    var latestBuildUserId: String = "",
    @ApiModelProperty("是否从模板中实例化出来的", required = false)
    var instanceFromTemplate: Boolean? = null,
    @ApiModelProperty("模板ID", required = false)
    var templateId: String? = null,
    @ApiModelProperty("版本名称", required = false)
    var versionName: String? = null,
    @ApiModelProperty("版本", required = false)
    var version: Long? = null,
    @ApiModelProperty("流水线更新人", required = false)
    val updater: String,
    @ApiModelProperty("流水线创建人", required = false)
    val creator: String,
    @ApiModelProperty("流水线分组和标签", required = false)
    var groupLabel: List<PipelineGroupLabels>? = null,
    @ApiModelProperty("最后自定义构建版本号", required = false)
    var latestBuildNumAlias: String? = null,
    @ApiModelProperty("自定义构建号规则", required = false)
    var buildNumRule: String? = null,
    @ApiModelProperty("编排详情", required = false)
    var model: Model? = null,
    @ApiModelProperty("流水线组名称列表", required = false)
    var viewNames: List<String>? = null,
    @ApiModelProperty("最后一次构建的构建信息", required = false)
    var lastBuildMsg: String? = null,
    @ApiModelProperty("最后一次构建所有的任务个数", required = false)
    var lastBuildTotalCount: Int? = null,
    @ApiModelProperty("最后一次构建已完成的任务个数", required = false)
    var lastBuildFinishCount: Int? = null,
    @ApiModelProperty("触发方式", required = false)
    var trigger: String? = null,
    @ApiModelProperty("webhook仓库别名", required = false)
    var webhookAliasName: String? = null,
    @ApiModelProperty("webhook提交信息", required = false)
    var webhookMessage: String? = null,
    @ApiModelProperty("webhook仓库地址", required = false)
    var webhookRepoUrl: String? = null,
    @ApiModelProperty("webhook类型", required = false)
    var webhookType: String? = null,
    @ApiModelProperty("是否已删除", required = false)
    var delete: Boolean? = false,
    @ApiModelProperty("流水线权限", required = false)
    val permissions: PipelinePermissions? = null
)
