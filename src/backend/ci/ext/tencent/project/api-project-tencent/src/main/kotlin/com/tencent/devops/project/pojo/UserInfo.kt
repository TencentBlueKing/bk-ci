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

package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户信息-公共账号必须绑定bg")
data class UserInfo(
    @get:Schema(title = "用户Id")
    val userId: String,
    @get:Schema(title = "用户名")
    val name: String,
    @get:Schema(title = "BgId")
    val bgId: Int,
    @get:Schema(title = "Bg名称")
    val bgName: String,
    @get:Schema(title = "业务线名称")
    val businessLineName: String? = null,
    @get:Schema(title = "业务线ID")
    val businessLineId: String? = null,
    @get:Schema(title = "部门Id")
    val deptId: Int?,
    @get:Schema(title = "部门名称")
    val deptName: String?,
    @get:Schema(title = "中心Id")
    val centerId: Int?,
    @get:Schema(title = "中心名称")
    val centerName: String?,
    @get:Schema(title = "组Id")
    val groupId: Int?,
    @get:Schema(title = "组名称")
    val groupName: String?
)
