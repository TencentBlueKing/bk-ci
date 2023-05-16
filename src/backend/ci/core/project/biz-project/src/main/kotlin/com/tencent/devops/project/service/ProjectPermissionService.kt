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

package com.tencent.devops.project.service

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.ResourceUpdateInfo

@Suppress("TooManyFunctions")
interface ProjectPermissionService {

    /**
     * 校验用户是否有这个项目的权限
     * @param accessToken 用于超级管理员绕过项目成员的限制，可为空
     */
    fun verifyUserProjectPermission(accessToken: String? = null, projectCode: String, userId: String): Boolean

    fun createResources(
        resourceRegisterInfo: ResourceRegisterInfo,
        authProjectCreateInfo: AuthProjectCreateInfo
    ): String

    fun deleteResource(projectCode: String)

    fun modifyResource(
        resourceUpdateInfo: ResourceUpdateInfo
    )

    fun getUserProjects(userId: String): List<String>

    fun getUserProjectsAvailable(userId: String): Map<String, String>

    fun filterProjects(
        userId: String,
        permission: AuthPermission
    ): List<String>?

    fun verifyUserProjectPermission(
        accessToken: String? = null,
        projectCode: String,
        userId: String,
        permission: AuthPermission
    ): Boolean

    fun cancelCreateAuthProject(
        userId: String,
        projectCode: String
    )

    fun cancelUpdateAuthProject(
        userId: String,
        projectCode: String
    )

    fun needApproval(needApproval: Boolean?): Boolean

    fun isShowUserManageIcon(): Boolean
}
