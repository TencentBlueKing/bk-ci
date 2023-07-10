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

package com.tencent.devops.project.service.impl

import com.tencent.devops.project.service.ProjectExtPermissionService

class RbacProjectExtPermissionServiceImpl : ProjectExtPermissionService {
    override fun verifyUserProjectPermission(
        accessToken: String,
        projectCode: String,
        userId: String
    ): Boolean {
        return true
    }

    override fun createUser2Project(
        createUser: String,
        userIds: List<String>,
        projectCode: String,
        roleId: Int?,
        roleName: String?,
        checkManager: Boolean
    ): Boolean {
        // 校验项目是否存在
        // 获取分级管理员id
        // 获取操作用户
        // 判断要加到那个组内
        // 1.若roleId和roleName都为空，加到开发人员，若roleId不为空，查询对应的组code。若roleName不为空，查询对应的组code
        // 若上一步找不到用户组，直接异常
        // 校验用户是否为真实用户
        // 添加用户到组   该接口字段：userId、projectId、roleCode、members、
        return true
    }

    override fun grantInstancePermission(
        userId: String,
        projectId: String,
        action: String,
        resourceType: String,
        resourceCode: String,
        userList: List<String>
    ): Boolean {
        return true
    }
}
