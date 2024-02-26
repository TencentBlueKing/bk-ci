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

interface IWorkspace {
    val workspaceId: Long?
    val workspaceName: String
    val projectId: String?
    val displayName: String?
    val status: WorkspaceStatus?
    val lastStatusUpdateTime: Long?
    val sleepingTime: Long?
    val createUserId: String
    val owner: String?
    val ownerCN: String?
    val viewers: List<String>?
    val viewersCN: List<String>?
    val hostName: String?
    val workspaceMountType: WorkspaceMountType
    val workspaceSystemType: WorkspaceSystemType
    val winConfig: WindowsResourceTypeConfig?
    val gpu: Int
    val cpu: Int
    val memory: Int
    val disk: Int
    var currentLoginUsers: List<String>
}

@Schema(title = "工作空间信息")
data class Workspace(
    @get:Schema(title = "工作空间ID<只读>", readOnly = true)
    override val workspaceId: Long?,
    @get:Schema(title = "工作空间名称")
    override val workspaceName: String,
    @get:Schema(title = "项目ID")
    override val projectId: String?,
    @get:Schema(title = "工作空间备注名称")
    override val displayName: String? = null,
    @get:Schema(title = "工作空间状态<只读>", readOnly = true)
    override val status: WorkspaceStatus? = null,
    @get:Schema(title = "状态最近更新时间<只读>", readOnly = true)
    override val lastStatusUpdateTime: Long? = null,
    @get:Schema(title = "休眠时间<只读>", readOnly = true)
    override val sleepingTime: Long? = null,
    @get:Schema(title = "工作空间创建人<只读>", readOnly = true)
    override val createUserId: String,
    @get:Schema(title = "工作空间拥有人<只读>", readOnly = true)
    override val owner: String? = null,
    @get:Schema(title = "工作空间拥有人CN<只读>", readOnly = true)
    override val ownerCN: String? = null,
    @get:Schema(title = "查看者")
    override val viewers: List<String>? = null,
    @get:Schema(title = "查看者_CN")
    override val viewersCN: List<String>? = null,
    @get:Schema(title = "工作空间对应的IP")
    override val hostName: String? = null,
    @get:Schema(title = "挂载平台类型")
    override val workspaceMountType: WorkspaceMountType,
    @get:Schema(title = "操作系统类型")
    override val workspaceSystemType: WorkspaceSystemType,
    @get:Schema(title = "windows 资源配置")
    override val winConfig: WindowsResourceTypeConfig? = null,
    override val gpu: Int = 0,
    override val cpu: Int = 8,
    override val memory: Int = 32,
    override val disk: Int = 100,
    @get:Schema(title = "当前登陆者信息")
    override var currentLoginUsers: List<String> = emptyList(),
    @get:Schema(title = "windows 地域配置")
    val zoneConfig: WindowsResourceZoneConfig? = null,
    @get:Schema(title = "远程开发仓库地址")
    val repositoryUrl: String? = null,
    @get:Schema(title = "仓库分支")
    val branch: String? = null,
    @get:Schema(title = "devfile配置路径")
    val devFilePath: String? = null,
    @get:Schema(title = "devfile 内容")
    val yaml: String? = null,
    @get:Schema(title = "工作空间模板ID")
    val wsTemplateId: Int? = null,
    @get:Schema(title = "工作空间操作路径")
    val workPath: String? = null,
    @get:Schema(title = "工作空间默认打开工程相对路径，默认根目录")
    val workspaceFolder: String? = null,
    @get:Schema(title = "镜像id")
    val imageId: String = "",
    @get:Schema(title = "工作空间归属")
    val ownerType: WorkspaceOwnerType,
    @get:Schema(title = "当前用户在此工作空间分配的人员类型")
    val assignType: WorkspaceShared.AssignType = WorkspaceShared.AssignType.OWNER,
    @get:Schema(title = "windows资源配置id")
    val winConfigId: Int? = null
) : IWorkspace
