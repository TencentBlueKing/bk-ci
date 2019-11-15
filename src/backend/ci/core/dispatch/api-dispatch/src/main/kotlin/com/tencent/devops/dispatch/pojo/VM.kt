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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("VM-基本信息")
data class VM(
    @ApiModelProperty("VM ID", required = true)
    val id: Int,
    @ApiModelProperty("VM 对应母机ID", required = true)
    val machineId: Int,
    @ApiModelProperty("VM 类型ID", required = true)
    val typeId: Int,
    @ApiModelProperty("VM IP地址", required = true)
    val ip: String,
    @ApiModelProperty("VM 名称", required = true)
    val name: String,
    @ApiModelProperty("VM 系统信息", required = true)
    val os: String,
    @ApiModelProperty("VM 系统信息版本", required = true)
    val osVersion: String,
    @ApiModelProperty("VM CPU信息", required = true)
    val cpu: String,
    @ApiModelProperty("VM 内存信息", required = true)
    val memory: String,
    @ApiModelProperty("VM 是否在维护状态", required = true)
    val inMaintain: Boolean,
    @ApiModelProperty("VM 管理员用户名", required = true)
    val vmManagerUsername: String,
    @ApiModelProperty("VM 管理员密码", required = true)
    val vmManagerPassword: String,
    @ApiModelProperty("VM 非管理员用户名", required = true)
    val vmUsername: String,
    @ApiModelProperty("VM 非管理员密码", required = true)
    val vmPassword: String,
    @ApiModelProperty("创建时间", required = true)
    val createdTime: Long,
    @ApiModelProperty("修改时间", required = true)
    val updatedTime: Long
)
