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

@Schema(title = "环境信息(权限)")
data class EnvWithPermission(
    @get:Schema(title = "环境 HashId", required = true)
    val envHashId: String,
    @get:Schema(title = "环境名称", required = true)
    val name: String,
    @get:Schema(title = "环境描述", required = true)
    val desc: String,
    @get:Schema(title = "环境类型（开发环境{DEV}|测试环境{TEST}|构建环境{BUILD}）", required = true)
    val envType: String,
    @get:Schema(title = "节点数量", required = false)
    val nodeCount: Int?,
    @get:Schema(title = "环境变量", required = true)
    val envVars: List<EnvVar>?,
    @get:Schema(title = "创建人", required = true)
    val createdUser: String,
    @get:Schema(title = "创建时间", required = true)
    val createdTime: Long,
    @get:Schema(title = "更新人", required = true)
    val updatedUser: String,
    @get:Schema(title = "更新时间", required = true)
    val updatedTime: Long,
    @get:Schema(title = "是否可以编辑", required = false)
    val canEdit: Boolean?,
    @get:Schema(title = "是否可以删除", required = false)
    val canDelete: Boolean?,
    @get:Schema(title = "是否可以使用", required = false)
    val canUse: Boolean?,
    @get:Schema(title = "项目名称", required = false)
    val projectName: String?
)
