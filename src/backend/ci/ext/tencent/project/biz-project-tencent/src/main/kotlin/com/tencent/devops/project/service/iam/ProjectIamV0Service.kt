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
 *
 */

package com.tencent.devops.project.service.iam

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectIamV0Service @Autowired constructor(
    private val bkAuthProjectApi: AuthProjectApi,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val tofService: TOFService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao
) {

    fun createUser2Project(
        createUser: String,
        userIds: List<String>,
        projectCode: String,
        roleId: Int?,
        roleName: String?
    ): Boolean {
        logger.info("[createUser2Project] createUser[$createUser] userId[$userIds] projectCode[$projectCode]")

        if (!bkAuthProjectApi.checkProjectManager(createUser, bsPipelineAuthServiceCode, projectCode)) {
            logger.warn("BKSystemMonitor| createUser2Project| $createUser is not manager for project[$projectCode]")
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.NOT_MANAGER,
                params = arrayOf(createUser, projectCode)
            )
        }
        return createUser2ProjectImpl(
            userIds = userIds,
            projectId = projectCode,
            roleId = roleId,
            roleName = roleName
        )
    }

    @Suppress("ALL")
    fun createPermission(
        userId: String,
        userList: List<String>?,
        projectId: String,
        permission: String,
        resourceType: String,
        authServiceCode: AuthServiceCode,
        resourceTypeCode: String
    ): Boolean {
        projectDao.getByEnglishName(dslContext, projectId)
            ?: throw OperationException(I18nUtil.getCodeLanMessage(
                messageCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                language = I18nUtil.getLanguage(userId)
            ))

        val authPermission = AuthPermission.get(permission)
        val authResourceType = AuthResourceType.get(resourceType)

        return bkAuthPermissionApi.addResourcePermissionForUsers(
            userId = userId,
            projectCode = projectId,
            permission = authPermission,
            serviceCode = authServiceCode,
            resourceType = authResourceType,
            resourceCode = resourceTypeCode,
            userIdList = userList ?: emptyList(),
            supplier = null
        )
    }

    fun createUser2ProjectImpl(
        userIds: List<String>,
        projectId: String,
        roleId: Int?,
        roleName: String?
    ): Boolean {
        logger.info("[createUser2Project] [$userIds] [$projectId] [$roleId] [$roleName]")
        val projectInfo = projectDao.getByEnglishName(dslContext, projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            defaultMessage = I18nUtil.getCodeLanMessage(
                messageCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = null
            )
        )
        val roleList = bkAuthProjectApi.getProjectRoles(bsPipelineAuthServiceCode, projectId, projectInfo.englishName)
        var authRoleId: String? = BkAuthGroup.DEVELOPER.value
        roleList.forEach {
            if (roleId == null && roleName.isNullOrEmpty()) {
                if (it.roleName == BkAuthGroup.DEVELOPER.value) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
            if (roleId != null) {
                if (it.roleId == roleId) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
            if (roleName != null) {
                if (it.roleName == roleName) {
                    authRoleId = it.roleId.toString()
                    return@forEach
                }
            }
        }
        userIds.forEach {
            try {
                tofService.getStaffInfo(it)
                bkAuthProjectApi.createProjectUser(
                    user = it,
                    serviceCode = bsPipelineAuthServiceCode,
                    projectCode = projectInfo.projectId,
                    role = authRoleId!!
                )
            } catch (ignore: OperationException) {
                logger.warn("OperationException $it $projectId ${ignore.message}")
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProjectMessageCode.QUERY_USER_INFO_FAIL,
                        defaultMessage = ignore.message,
                        params = arrayOf(it)
                    )
                )
            } catch (ignore: Exception) {
                logger.warn("createUser2Project fail, userId[$it]", ignore)
                return false
            }
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectIamV0Service::class.java)
    }
}
