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

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

interface WorkspaceRecordInf {
    val workspaceId: Long
    val projectId: String
    val workspaceName: String
    val workspaceSystemType: WorkspaceSystemType
    val createUserId: String
    val displayName: String
    val templateId: Int?
    val repositoryUrl: String?
    val branch: String?
    val yaml: String?
    val devFilePath: String?
    val dockerFile: String
    val imagePath: String
    val workPath: String?
    val workspaceFolder: String?
    val hostName: String?
    val gpu: Int
    val cpu: Int
    val memory: Int
    val usageTime: Int
    val sleepingTime: Int
    val disk: Int
    val creatorBgName: String
    val creatorDeptName: String
    val creatorCenterName: String
    val creatorGroupName: String
    val status: WorkspaceStatus
    val createTime: LocalDateTime
    val updateTime: LocalDateTime
    val lastStatusUpdateTime: LocalDateTime?
    val preciAgentId: String?
    val workspaceMountType: WorkspaceMountType
    val ownerType: WorkspaceOwnerType
    val remark: String?
}

@Schema(title = "工作空间信息")
data class WorkspaceRecord(
    @get:Schema(title = "工作空间ID<只读>")
    override val workspaceId: Long,
    @get:Schema(title = "项目ID")
    override val projectId: String,
    @get:Schema(title = "工作空间名称")
    override val workspaceName: String,
    @get:Schema(title = "工作空间备注名称")
    override val displayName: String,
    @get:Schema(title = "工作空间模板ID")
    override val templateId: Int?,
    @get:Schema(title = "远程开发仓库地址")
    override val repositoryUrl: String?,
    @get:Schema(title = "仓库分支")
    override val branch: String?,
    @get:Schema(title = "devfile 内容")
    override val yaml: String?,
    @get:Schema(title = "devfile配置路径")
    override val devFilePath: String?,
    @get:Schema(title = "依赖镜像的DockerFile内容")
    override val dockerFile: String,
    @get:Schema(title = "镜像地址")
    override val imagePath: String,
    @get:Schema(title = "工作空间操作路径")
    override val workPath: String?,
    @get:Schema(title = "工作空间默认打开工程相对路径，默认根目录")
    override val workspaceFolder: String?,
    @get:Schema(title = "工作空间对应的IP")
    override val hostName: String?,
    override val gpu: Int,
    override val cpu: Int,
    override val memory: Int,
    @get:Schema(title = "已使用时间,单位:s（容器结束时更新）")
    override val usageTime: Int,
    @get:Schema(title = "休眠时间<只读>")
    override val sleepingTime: Int,
    override val disk: Int,
    @get:Schema(title = "工作空间创建人")
    override val createUserId: String,
    @get:Schema(title = "所在事业群，用作度量统计")
    override val creatorBgName: String,
    @get:Schema(title = "所在部门，用作度量统计")
    override val creatorDeptName: String,
    @get:Schema(title = "所在中心，用作度量统计")
    override val creatorCenterName: String,
    @get:Schema(title = "所在组，用作度量统计")
    override val creatorGroupName: String,
    @get:Schema(title = "工作空间状态<只读>")
    override val status: WorkspaceStatus,
    @get:Schema(title = "工作空间状态<只读>")
    override val createTime: LocalDateTime,
    @get:Schema(title = "工作空间状态<只读>")
    override val updateTime: LocalDateTime,
    @get:Schema(title = "工作空间状态<只读>")
    override val lastStatusUpdateTime: LocalDateTime?,
    @get:Schema(title = "preci go-agent id")
    override val preciAgentId: String?,
    @get:Schema(title = "挂载平台类型")
    override val workspaceMountType: WorkspaceMountType,
    @get:Schema(title = "操作系统类型")
    override val workspaceSystemType: WorkspaceSystemType,
    @get:Schema(title = "工作空间归属")
    override val ownerType: WorkspaceOwnerType,
    @get:Schema(title = "工作空间备注")
    override val remark: String?
) : WorkspaceRecordInf

/**
 * 需要与WorkspaceRecord同步修改
 */
data class WorkspaceRecordWithDetail(
    @get:Schema(title = "工作空间ID<只读>")
    override val workspaceId: Long,
    @get:Schema(title = "项目ID")
    override val projectId: String,
    @get:Schema(title = "工作空间名称")
    override val workspaceName: String,
    @get:Schema(title = "工作空间备注名称")
    override val displayName: String,
    @get:Schema(title = "工作空间模板ID")
    override val templateId: Int?,
    @get:Schema(title = "远程开发仓库地址")
    override val repositoryUrl: String?,
    @get:Schema(title = "仓库分支")
    override val branch: String?,
    @get:Schema(title = "devfile 内容")
    override val yaml: String?,
    @get:Schema(title = "devfile配置路径")
    override val devFilePath: String?,
    @get:Schema(title = "依赖镜像的DockerFile内容")
    override val dockerFile: String,
    @get:Schema(title = "镜像地址")
    override val imagePath: String,
    @get:Schema(title = "工作空间操作路径")
    override val workPath: String?,
    @get:Schema(title = "工作空间默认打开工程相对路径，默认根目录")
    override val workspaceFolder: String?,
    @get:Schema(title = "工作空间对应的IP")
    override val hostName: String?,
    override val gpu: Int,
    override val cpu: Int,
    override val memory: Int,
    @get:Schema(title = "已使用时间,单位:s（容器结束时更新）")
    override val usageTime: Int,
    @get:Schema(title = "休眠时间<只读>")
    override val sleepingTime: Int,
    override val disk: Int,
    @get:Schema(title = "工作空间创建人")
    override val createUserId: String,
    @get:Schema(title = "所在事业群，用作度量统计")
    override val creatorBgName: String,
    @get:Schema(title = "所在部门，用作度量统计")
    override val creatorDeptName: String,
    @get:Schema(title = "所在中心，用作度量统计")
    override val creatorCenterName: String,
    @get:Schema(title = "所在组，用作度量统计")
    override val creatorGroupName: String,
    @get:Schema(title = "工作空间状态<只读>")
    override val status: WorkspaceStatus,
    @get:Schema(title = "工作空间状态<只读>")
    override val createTime: LocalDateTime,
    @get:Schema(title = "工作空间状态<只读>")
    override val updateTime: LocalDateTime,
    @get:Schema(title = "工作空间状态<只读>")
    override val lastStatusUpdateTime: LocalDateTime?,
    @get:Schema(title = "preci go-agent id")
    override val preciAgentId: String?,
    @get:Schema(title = "挂载平台类型")
    override val workspaceMountType: WorkspaceMountType,
    @get:Schema(title = "操作系统类型")
    override val workspaceSystemType: WorkspaceSystemType,
    @get:Schema(title = "工作空间归属")
    override val ownerType: WorkspaceOwnerType,
    @get:Schema(title = " 工作空间详情")
    val workSpaceDetail: String,
    @get:Schema(title = "工作空间备注")
    override val remark: String?
) : WorkspaceRecordInf
