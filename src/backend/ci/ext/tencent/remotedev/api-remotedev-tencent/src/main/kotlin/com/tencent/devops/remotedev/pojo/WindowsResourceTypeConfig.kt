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

@ApiModel("WINDOWS GPU资源配置表")
data class WindowsResourceTypeConfig(
    @ApiModelProperty("Id")
    val id: Long?,
    @ApiModelProperty("是否可用")
    val available: Boolean?,
    @ApiModelProperty("资源类型：M，L，XL，S")
    val size: String,
    @ApiModelProperty("GPU卡类型")
    val type: String? = null,
    @ApiModelProperty("GPU")
    val gpu: Int,
    @ApiModelProperty("vGPU")
    val vgpu: String? = "",
    @ApiModelProperty("CPU")
    val cpu: Int,
    @ApiModelProperty("vCPU")
    val vcpu: String? = "",
    @ApiModelProperty("内存")
    val memory: Int,
    @ApiModelProperty("独享内存")
    val vmemory: String? = "",
    @ApiModelProperty("数据盘，本地SSD盘")
    val disk: String,
    @ApiModelProperty("云SSD盘")
    val hdisk: String,
    @ApiModelProperty("系统盘，本地SSD")
    val sdisk: String,
    @ApiModelProperty("权重，用于页面展示先后顺序")
    val weight: Int? = 0,
    @ApiModelProperty("描述")
    val description: String
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
