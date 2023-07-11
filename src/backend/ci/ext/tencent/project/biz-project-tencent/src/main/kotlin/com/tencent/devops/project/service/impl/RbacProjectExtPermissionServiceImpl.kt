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
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.service.ProjectExtPermissionService
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class RbacProjectExtPermissionServiceImpl constructor(
    val client: Client,
    val tokenService: ClientTokenService,
    val projectDao: ProjectDao,
    val dslContext: DSLContext,
    val tofService: TOFService
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
        projectDao.getByEnglishName(dslContext, projectCode)
            ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST
            )
        if (checkManager) {
            val isProjectManager = client.get(ServiceProjectAuthResource::class).checkProjectManager(
                token = tokenService.getSystemToken(null)!!,
                userId = createUser,
                projectCode = projectCode,
            ).data
            if (!isProjectManager!!) {
                logger.warn("BKSystemMonitor| createUser2Project| $createUser is not manager for project[$projectCode]")
                throw ErrorCodeException(
                    errorCode = ProjectMessageCode.NOT_MANAGER,
                    params = arrayOf(createUser, projectCode)
                )
            }
        }
        val roleCode = roleId?.let { BkAuthGroup.getByRoleId(it).value }
            ?: roleName?.let { BkAuthGroup.get(it).value }
            ?: BkAuthGroup.DEVELOPER.value
        // 校验用户是否存在
        userIds.forEach {
            try {
                tofService.getStaffInfo(it)
            } catch (ope: OperationException) {
                logger.warn("getStaffInfo fail $it $projectCode")
                throw ope
            } catch (ignore: Exception) {
                logger.warn("getStaffInfo fail, userId[$it]", ignore)
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProjectMessageCode.QUERY_USER_INFO_FAIL,
                        defaultMessage = ignore.message,
                        params = arrayOf(it)
                    )
                )
            }
        }
        client.get(ServiceProjectAuthResource::class).batchCreateProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = createUser,
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
