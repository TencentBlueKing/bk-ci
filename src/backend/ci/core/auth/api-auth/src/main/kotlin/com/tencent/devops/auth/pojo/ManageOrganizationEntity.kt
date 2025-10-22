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

package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class ManageOrganizationEntity(
    @get:Schema(title = "授权Id")
    val id: Int,
    @get:Schema(title = "授权名称")
    val name: String,
    @get:Schema(title = "授权策略Id")
    val strategyId: Int,
    @get:Schema(title = "授权策略Name")
    val strategyName: String,
    @get:Schema(title = "组织名称")
    val organizationName: String,
    @get:Schema(title = "组织Id")
    val organizationId: Int,
    @get:Schema(title = "组织级别")
    val organizationLevel: Int,
    @get:Schema(title = "父级组织信息")
    val parentOrganizations: List<OrganizationEntity>?,
    @get:Schema(title = "用户数")
    var userCount: Int? = 0,
    @get:Schema(title = "添加人")
    val createUser: String,
    @get:Schema(title = "添加时间")
    val createTime: String
)
