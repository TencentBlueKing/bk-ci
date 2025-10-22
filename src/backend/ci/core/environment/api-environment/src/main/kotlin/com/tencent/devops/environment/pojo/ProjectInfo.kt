/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.environment.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "VM虚拟机配额")
data class ProjectInfo(
    @get:Schema(title = "允许使用BCS虚拟机功能", required = true)
    val bcsVmEnabled: Boolean,
    @get:Schema(title = "BCS虚拟机配额", required = true)
    val bcsVmQuota: Int,
    @get:Schema(title = "BCS虚拟机已用数量", required = true)
    val bcsVmUsedCount: Int,
    @get:Schema(title = "BCS虚拟机可用数量", required = true)
    val bcsVmRestCount: Int,
    @get:Schema(title = "导入服务器配额", required = true)
    val importQuota: Int,
    @get:Schema(title = "允许使用DevCloud虚拟机功能", required = true)
    val devCloudVmEnabled: Boolean,
    @get:Schema(title = "DevCloud虚拟机配额", required = true)
    val devCloudVmQuota: Int,
    @get:Schema(title = "DevCloud虚拟机已用数量", required = true)
    val devCloudVmUsedCount: Int
)
