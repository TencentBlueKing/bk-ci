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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.BK_DEVOPS_SCOPE
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.project.service.ProjectPermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectPermissionServiceImpl @Autowired constructor(
    private val bkAuthProjectApi: AuthProjectApi,
    private val bkAuthResourceApi: AuthResourceApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode
) : ProjectPermissionService {

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        return bkAuthProjectApi.getUserProjectsAvailable(projectAuthServiceCode, userId, null)
    }

    override fun getUserProjects(userId: String): List<String> {
        return bkAuthProjectApi.getUserProjects(projectAuthServiceCode, userId, null)
    }

    override fun modifyResource(projectCode: String, projectName: String) {
        bkAuthResourceApi.modifyResource(
            serviceCode = projectAuthServiceCode,
            resourceType = BkAuthResourceType.PROJECT,
            projectCode = BK_DEVOPS_SCOPE,
            resourceCode = projectCode,
            resourceName = projectName
        )
    }

    override fun deleteResource(projectCode: String) {

        bkAuthResourceApi.deleteResource(
            serviceCode = projectAuthServiceCode,
            resourceType = BkAuthResourceType.PROJECT,
            projectCode = BK_DEVOPS_SCOPE,
            resourceCode = projectCode
        )
    }

    override fun createResources(userId: String, projectList: List<ResourceRegisterInfo>) {
        bkAuthResourceApi.batchCreateResource(
            serviceCode = projectAuthServiceCode,
            resourceType = BkAuthResourceType.PROJECT,
            resourceList = projectList,
            projectCode = BK_DEVOPS_SCOPE,
            user = userId
        )
    }

    override fun verifyUserProjectPermission(projectCode: String, userId: String): Boolean {
        val projectCodes = bkAuthProjectApi.getUserProjects(projectAuthServiceCode, userId, null)
        return projectCodes.contains(projectCode)
    }
}