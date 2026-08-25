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
 *
 */

package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class ProjectCreateUserInfo(
    @get:Schema(title = "操作人:发起本次添加操作的用户 ID，一般传当前操作者即可。")
    val createUserId: String?,
    @get:Schema(title = "角色名称:要添加到的角色名称，例如 manager、developer、viewer、executor。已知 groupId 时可不传。")
    val roleName: String?,
    @get:Schema(
        title = "角色Id:历史角色 ID，仅兼容旧版调用。可选值：0=visitor，2=manager，" +
                "4=developer，5=maintainer，6=pm，7=qc，8=tester。已传 groupId 或 roleName 时可不传。"
    )
    val roleId: Int?,
    @get:Schema(title = "组ID:用户组 ID。已知具体用户组时，优先传该字段。")
    val groupId: Int?,
    @get:Schema(title = "目标用户:要加入用户组的用户 ID 列表，支持批量传入。")
    val userIds: List<String>? = emptyList(),
    @get:Schema(title = "目标部门:要加入用户组的部门 ID 列表，支持批量传入。")
    val deptIds: List<String>? = emptyList(),
    @get:Schema(title = "资源类型:资源类型，例如 project、pipeline。未传 groupId 时建议填写；不传默认按 project 处理。")
    val resourceType: String?,
    @get:Schema(title = "资源ID：如流水线id等。未传 groupId 时建议填写。")
    val resourceCode: String?,
    @get:Schema(title = "过期天数：成员有效期，单位为天。例如传 30 表示 30 天后过期；不传默认 365 天。")
    val expiredTime: Long? = null
)
