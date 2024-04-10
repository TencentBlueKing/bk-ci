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

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.service.ProjectExtPermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class RbacProjectExtPermissionServiceImpl constructor(
    val client: Client,
    val tokenService: ClientTokenService,
    val projectDao: ProjectDao,
    val dslContext: DSLContext,
    val authProjectApi: AuthProjectApi,
    val projectAuthServiceCode: ProjectAuthServiceCode
) : ProjectExtPermissionService {
    companion object {
        val logger = LoggerFactory.getLogger(RbacProjectExtPermissionServiceImpl::class.java)
    }

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
        val projectInfo = (projectDao.getByEnglishName(dslContext, projectCode)
            ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST
            ))
        val currencyCreateUser = if (!checkManager || createUser.isBlank()) {
            projectInfo.creator
        } else {
            createUser
        }
        if (checkManager) {
            val isProjectManager = authProjectApi.checkProjectManager(
                userId = currencyCreateUser,
                serviceCode = projectAuthServiceCode,
                projectCode = projectCode
            )
            if (!isProjectManager) {
                logger.warn(
                    "BKSystemMonitor| createUser2Project| " +
                        "$currencyCreateUser is not manager for project[$projectCode]"
                )
                throw ErrorCodeException(
                    errorCode = ProjectMessageCode.NOT_MANAGER,
                    params = arrayOf(currencyCreateUser, projectCode)
                )
            }
        }
        val roleCode = roleId?.let { BkAuthGroup.getByRoleId(it).value }
            ?: roleName?.let { BkAuthGroup.get(it).value }
            ?: BkAuthGroup.DEVELOPER.value
        client.get(ServiceProjectAuthResource::class).batchCreateProjectUser(
            token = tokenService.getSystemToken()!!,
            userId = currencyCreateUser,
            projectCode = projectCode,
            roleCode = roleCode,
            members = userIds
        )
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
