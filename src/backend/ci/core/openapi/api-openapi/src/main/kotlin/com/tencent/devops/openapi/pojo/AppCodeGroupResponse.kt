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
package com.tencent.devops.openapi.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "AppCode所属组织架构信息-response")
data class AppCodeGroupResponse(
    @get:Schema(title = "ID")
    val id: Long,
    @get:Schema(title = "appCode")
    val appCode: String,
    @get:Schema(title = "事业群ID")
    val bgId: Int?,
    @get:Schema(title = "事业群名字")
    val bgName: String?,
    @get:Schema(title = "部门ID")
    val deptId: Int?,
    @get:Schema(title = "部门名字")
    val deptName: String?,
    @get:Schema(title = "中心ID")
    val centerId: Int?,
    @get:Schema(title = "中心名字")
    val centerName: String?,
    @get:Schema(title = "创建人")
    val creator: String?,
    @get:Schema(title = "创建时间")
    val createTime: Long?,
    @get:Schema(title = "更新人")
    val updater: String?,
    @get:Schema(title = "更新时间")
    val updateTime: Long?
)
