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

@Schema(title = "云桌面分组成员变更")
data class WorkspaceGroupAssignment(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "工作空间名称列表")
    val workspaceNames: List<String>
)


