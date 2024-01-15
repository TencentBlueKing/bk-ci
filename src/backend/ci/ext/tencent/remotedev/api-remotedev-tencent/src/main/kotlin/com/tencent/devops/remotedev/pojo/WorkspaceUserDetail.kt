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

@Schema(description = "获取用户工作空间详情model")
data class WorkspaceUserDetail(
    @Schema(description = "运行中容器数量")
    val runningCount: Int,
    @Schema(description = "已休眠容器数量")
    val sleepingCount: Int,
    @Schema(description = "已销毁容器数量")
    val deleteCount: Int,
    @Schema(description = "计费时间（秒）")
    val chargeableTime: Long,
    @Schema(description = "使用时间（秒）")
    val usageTime: Long,
    @Schema(description = "休眠时间（秒）")
    val sleepingTime: Long,
    @Schema(description = "免费时间（秒）")
    val discountTime: Long,
    @Schema(description = "CPU 核心数")
    val cpu: Int,
    @Schema(description = "内存大小（MB）")
    val memory: Int,
    @Schema(description = "存储空间大小（GB）")
    val disk: Int,
    @Schema(description = "windows-gpu体验剩余时长(秒)")
    val winUsageTimeLeft: Int
)
