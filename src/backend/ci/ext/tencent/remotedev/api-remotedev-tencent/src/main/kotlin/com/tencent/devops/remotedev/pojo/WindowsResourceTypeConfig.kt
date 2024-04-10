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

@Schema(title = "WINDOWS GPU资源配置表")
data class WindowsResourceTypeConfig(
    @get:Schema(title = "Id")
    val id: Long?,
    @get:Schema(title = "是否可用")
    val available: Boolean?,
    @get:Schema(title = "资源类型：M，L，XL，S")
    val size: String,
    @get:Schema(title = "GPU卡类型")
    val type: String? = null,
    @get:Schema(title = "GPU")
    val gpu: Int,
    @get:Schema(title = "vGPU")
    val vgpu: String? = "",
    @get:Schema(title = "CPU")
    val cpu: Int,
    @get:Schema(title = "vCPU")
    val vcpu: String? = "",
    @get:Schema(title = "内存")
    val memory: Int,
    @get:Schema(title = "独享内存")
    val vmemory: String? = "",
    @get:Schema(title = "数据盘，本地SSD盘")
    val disk: String,
    @get:Schema(title = "云SSD盘")
    val hdisk: String,
    @get:Schema(title = "系统盘，本地SSD")
    val sdisk: String,
    @get:Schema(title = "权重，用于页面展示先后顺序")
    val weight: Int? = 0,
    @get:Schema(title = "描述")
    val description: String,
    @get:Schema(title = "是否是特殊机型")
    val specModel: Boolean = false
) {

    companion object {
        const val GB = "GB"
        const val TB = "TB"
    }

    fun workspaceDisk() = transferGb(disk)

    private fun transferGb(disk: String): Int = when {
        disk.contains(GB) -> disk.removeSuffix(GB).trim().toIntOrNull() ?: 0
        disk.contains(TB) -> (disk.removeSuffix(TB).trim().toIntOrNull() ?: 0) * 1024
        else -> 0
    }
}
