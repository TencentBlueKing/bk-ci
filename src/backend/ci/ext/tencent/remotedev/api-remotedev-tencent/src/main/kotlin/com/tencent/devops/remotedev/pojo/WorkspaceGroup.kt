/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "云桌面分组")
data class WorkspaceGroup(
    @get:Schema(title = "分组ID")
    val id: Long,
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "分组名称")
    val name: String,
    @get:Schema(title = "描述")
    val description: String?,
    @get:Schema(title = "创建时间")
    val createTime: Long,
    @get:Schema(title = "更新时间")
    val updateTime: Long,
    @get:Schema(title = "创建者")
    val createUser: String,
    @get:Schema(title = "更新者")
    val updateUser: String,
    @get:Schema(title = "分组下工作空间数量")
    val workspaceCount: Int
)

@Schema(title = "分组工作空间简要信息")
data class WorkspaceGroupItem(
    @get:Schema(title = "工作空间名称")
    val workspaceName: String,
    @get:Schema(title = "创建时间")
    val createTime: Long
)


