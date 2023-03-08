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

package com.tencent.devops.auth.service.v0

import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.code.BSCommonAuthServiceCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V0AuthPermissionProjectServiceImpl @Autowired constructor(
    private val authProjectApi: AuthProjectApi,
    val authServiceCode: BSCommonAuthServiceCode
) : PermissionProjectService {

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {

        return authProjectApi.getProjectUsers(
            serviceCode = authServiceCode,
            projectCode = projectCode,
            group = group
        )
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        return authProjectApi.getProjectGroupAndUserList(
            serviceCode = authServiceCode,
            projectCode = projectCode
        )
    }

    override fun getUserProjects(userId: String): List<String> {
        return authProjectApi.getUserProjects(
            serviceCode = authServiceCode,
            userId = userId,
            supplier = null
        )
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        return authProjectApi.isProjectUser(
            user = userId,
            projectCode = projectCode,
            group = group,
            serviceCode = authServiceCode
        )
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return authProjectApi.checkProjectManager(
            userId = userId,
            projectCode = projectCode,
            serviceCode = authServiceCode
        )
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return authProjectApi.getProjectRoles(
            serviceCode = authServiceCode,
            projectCode = projectCode,
            projectId = projectId
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(V0AuthPermissionProjectServiceImpl::class.java)
    }
}
