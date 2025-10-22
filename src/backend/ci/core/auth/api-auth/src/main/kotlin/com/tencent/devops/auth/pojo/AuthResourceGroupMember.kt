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
import java.time.LocalDateTime

@Schema(title = "资源用户组成员信息")
data class AuthResourceGroupMember(
    val id: Long? = null,
    @get:Schema(title = "项目ID", required = true)
    val projectCode: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "资源ID", required = true)
    val resourceCode: String,
    @get:Schema(title = "资源ID", required = true)
    val groupCode: String,
    @get:Schema(title = "权限中心组ID", required = true)
    val iamGroupId: Int,
    @get:Schema(title = "成员ID, 用户: 英文名, 组织: 组织ID, 人员模板: 模板ID", required = true)
    val memberId: String,
    @get:Schema(title = "成员名", required = true)
    val memberName: String,
    @get:Schema(title = "成员类型, 用户/组织/人员模板", required = true)
    val memberType: String,
    @get:Schema(title = "过期时间", required = true)
    val expiredTime: LocalDateTime
)
