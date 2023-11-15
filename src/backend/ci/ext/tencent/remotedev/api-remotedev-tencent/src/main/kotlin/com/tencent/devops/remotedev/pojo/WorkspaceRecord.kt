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

package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

interface WorkspaceRecordInf {
    val workspaceName: String
    val workspaceSystemType: WorkspaceSystemType
    val createUserId: String
}

@ApiModel("工作空间信息")
data class WorkspaceRecord(
    @ApiModelProperty("工作空间ID<只读>")
    val workspaceId: Long,
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("工作空间名称")
    override val workspaceName: String,
    @ApiModelProperty("工作空间备注名称")
    val displayName: String,
    @ApiModelProperty("工作空间模板ID")
    val templateId: Int?,
    @ApiModelProperty("远程开发仓库地址")
    val repositoryUrl: String?,
    @ApiModelProperty("仓库分支")
    val branch: String?,
    @ApiModelProperty("devfile 内容")
    val yaml: String?,
    @ApiModelProperty("devfile配置路径")
    val devFilePath: String?,
    @ApiModelProperty("依赖镜像的DockerFile内容")
    val dockerFile: String,
    @ApiModelProperty("镜像地址")
    val imagePath: String,
    @ApiModelProperty("工作空间操作路径")
    val workPath: String?,
    @ApiModelProperty("工作空间默认打开工程相对路径，默认根目录")
    val workspaceFolder: String?,
    @ApiModelProperty("工作空间对应的IP")
    val hostName: String?,
    val gpu: Int,
    val cpu: Int,
    val memory: Int,
    @ApiModelProperty("已使用时间,单位:s（容器结束时更新）")
    val usageTime: Int,
    @ApiModelProperty("休眠时间<只读>")
    val sleepingTime: Int,
    val disk: Int,
    @ApiModelProperty("工作空间创建人")
    override val createUserId: String,
    @ApiModelProperty("所在事业群，用作度量统计")
    val creatorBgName: String,
    @ApiModelProperty("所在部门，用作度量统计")
    val creatorDeptName: String,
    @ApiModelProperty("所在中心，用作度量统计")
    val creatorCenterName: String,
    @ApiModelProperty("所在组，用作度量统计")
    val creatorGroupName: String,
    @ApiModelProperty("工作空间状态<只读>")
    val status: WorkspaceStatus,
    @ApiModelProperty("工作空间状态<只读>")
    val createTime: LocalDateTime,
    @ApiModelProperty("工作空间状态<只读>")
    val updateTime: LocalDateTime,
    @ApiModelProperty("工作空间状态<只读>")
    val lastStatusUpdateTime: LocalDateTime?,
    @ApiModelProperty("preci go-agent id")
    val preciAgentId: String?,
    @ApiModelProperty("挂载平台类型")
    val workspaceMountType: WorkspaceMountType,
    @ApiModelProperty("操作系统类型")
    override val workspaceSystemType: WorkspaceSystemType,
    @ApiModelProperty("工作空间归属")
    val ownerType: WorkspaceOwnerType
) : WorkspaceRecordInf

/**
 * 需要与WorkspaceRecord同步修改
 */
data class WorkspaceRecordWithDetail(
    @ApiModelProperty("工作空间ID<只读>")
    val workspaceId: Long,
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("工作空间名称")
    override val workspaceName: String,
    @ApiModelProperty("工作空间备注名称")
    val displayName: String,
    @ApiModelProperty("工作空间模板ID")
    val templateId: Int?,
    @ApiModelProperty("远程开发仓库地址")
    val repositoryUrl: String?,
    @ApiModelProperty("仓库分支")
    val branch: String?,
    @ApiModelProperty("devfile 内容")
    val yaml: String?,
    @ApiModelProperty("devfile配置路径")
    val devFilePath: String?,
    @ApiModelProperty("依赖镜像的DockerFile内容")
    val dockerFile: String,
    @ApiModelProperty("镜像地址")
    val imagePath: String,
    @ApiModelProperty("工作空间操作路径")
    val workPath: String?,
    @ApiModelProperty("工作空间默认打开工程相对路径，默认根目录")
    val workspaceFolder: String?,
    @ApiModelProperty("工作空间对应的IP")
    val hostName: String?,
    val gpu: Int,
    val cpu: Int,
    val memory: Int,
    @ApiModelProperty("已使用时间,单位:s（容器结束时更新）")
    val usageTime: Int,
    @ApiModelProperty("休眠时间<只读>")
    val sleepingTime: Int,
    val disk: Int,
    @ApiModelProperty("工作空间创建人")
    override val createUserId: String,
    @ApiModelProperty("所在事业群，用作度量统计")
    val creatorBgName: String,
    @ApiModelProperty("所在部门，用作度量统计")
    val creatorDeptName: String,
    @ApiModelProperty("所在中心，用作度量统计")
    val creatorCenterName: String,
    @ApiModelProperty("所在组，用作度量统计")
    val creatorGroupName: String,
    @ApiModelProperty("工作空间状态<只读>")
    val status: WorkspaceStatus,
    @ApiModelProperty("工作空间状态<只读>")
    val createTime: LocalDateTime,
    @ApiModelProperty("工作空间状态<只读>")
    val updateTime: LocalDateTime,
    @ApiModelProperty("工作空间状态<只读>")
    val lastStatusUpdateTime: LocalDateTime?,
    @ApiModelProperty("preci go-agent id")
    val preciAgentId: String?,
    @ApiModelProperty("挂载平台类型")
    val workspaceMountType: WorkspaceMountType,
    @ApiModelProperty("操作系统类型")
    override val workspaceSystemType: WorkspaceSystemType,
    @ApiModelProperty("工作空间归属")
    val ownerType: WorkspaceOwnerType,
    @ApiModelProperty(" 工作空间详情")
    val workSpaceDetail: String
) : WorkspaceRecordInf
