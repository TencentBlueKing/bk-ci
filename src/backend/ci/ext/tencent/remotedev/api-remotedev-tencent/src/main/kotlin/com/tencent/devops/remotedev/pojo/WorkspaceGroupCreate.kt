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

@Schema(title = "创建云桌面分组")
data class WorkspaceGroupCreate(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "分组名称")
    val name: String,
    @get:Schema(title = "描述")
    val description: String? = null
)


