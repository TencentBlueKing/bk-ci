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

package com.tencent.devops.auth.service.stream

import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import org.springframework.beans.factory.annotation.Autowired

class StreamPermissionProjectServiceImpl @Autowired constructor(
    private val streamPermissionService: StreamPermissionServiceImpl
) : PermissionProjectService {
    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        // stream场景下使用不到此接口。占做默认实现
        return emptyList()
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        // stream场景下使用不到此接口。占做默认实现
        return emptyList()
    }

    override fun getUserProjects(userId: String): List<String> {
        // stream场景下使用不到此接口。占做默认实现
        return emptyList()
    }

    override fun getUserProjectsByPermission(userId: String, action: String): List<String> {
        return emptyList()
    }

    override fun isProjectUser(
        userId: String,
        projectCode: String,
        group: BkAuthGroup?
    ): Boolean {
        return streamPermissionService.isProjectMember(projectCode, userId).first
    }

    override fun checkProjectManager(
        userId: String,
        projectCode: String
    ): Boolean {
        return streamPermissionService.isProjectMember(projectCode, userId).second
    }

    override fun createProjectUser(
        userId: String,
        projectCode: String,
        roleCode: String
    ): Boolean {
        // stream场景下使用不到此接口。占做默认实现
        return false
    }

    override fun getProjectRoles(
        projectCode: String,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        // stream场景下使用不到此接口。占做默认实现
        return emptyList()
    }
}
