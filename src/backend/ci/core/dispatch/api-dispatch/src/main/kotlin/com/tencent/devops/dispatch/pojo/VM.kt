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

package com.tencent.devops.dispatch.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "VM-基本信息")
data class VM(
    @get:Schema(title = "VM ID", required = true)
    val id: Long,
    @get:Schema(title = "VM 对应母机ID", required = true)
    val machineId: Int,
    @get:Schema(title = "VM 类型ID", required = true)
    val typeId: Int,
    @get:Schema(title = "VM IP地址", required = true)
    val ip: String,
    @get:Schema(title = "VM 名称", required = true)
    val name: String,
    @get:Schema(title = "VM 系统信息", required = true)
    val os: String,
    @get:Schema(title = "VM 系统信息版本", required = true)
    val osVersion: String,
    @get:Schema(title = "VM CPU信息", required = true)
    val cpu: String,
    @get:Schema(title = "VM 内存信息", required = true)
    val memory: String,
    @get:Schema(title = "VM 是否在维护状态", required = true)
    val inMaintain: Boolean,
    @get:Schema(title = "VM 管理员用户名", required = true)
    val vmManagerUsername: String,
    @get:Schema(title = "VM 管理员密码", required = true)
    val vmManagerPassword: String,
    @get:Schema(title = "VM 非管理员用户名", required = true)
    val vmUsername: String,
    @get:Schema(title = "VM 非管理员密码", required = true)
    val vmPassword: String,
    @get:Schema(title = "创建时间", required = true)
    val createdTime: Long,
    @get:Schema(title = "修改时间", required = true)
    val updatedTime: Long
)
