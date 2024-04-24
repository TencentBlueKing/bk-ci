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

@Schema(title = "获取指定工作空间详情model")
data class WorkspaceDetail(
    @get:Schema(title = "工作空间ID")
    val workspaceId: Long,
    @get:Schema(title = "工作空间名称")
    val workspaceName: String,
    @get:Schema(title = "工作空间备注名称")
    val displayName: String?,
    @get:Schema(title = "工作空间状态")
    val status: WorkspaceStatus,
    @get:Schema(title = "最近状态修改时间")
    val lastUpdateTime: Long,
    @get:Schema(title = "计费时间（秒）")
    val chargeableTime: Long,
    @get:Schema(title = "使用时间（秒）")
    val usageTime: Long,
    @get:Schema(title = "休眠时间（秒）")
    val sleepingTime: Long,
    @get:Schema(title = "CPU 核心数")
    val cpu: Int,
    @get:Schema(title = "内存大小（MB）")
    val memory: Int,
    @get:Schema(title = "存储空间大小（GB）")
    val disk: Int,
    @get:Schema(title = "yaml 配置内容")
    val yaml: String?,
    @get:Schema(title = "操作系统类型")
    val systemType: WorkspaceSystemType,
    @get:Schema(title = "挂载平台类型")
    val workspaceMountType: WorkspaceMountType,
    @get:Schema(title = "工作空间归属")
    val ownerType: WorkspaceOwnerType
)
