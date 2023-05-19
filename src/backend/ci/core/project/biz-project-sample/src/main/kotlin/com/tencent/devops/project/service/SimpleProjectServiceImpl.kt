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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.impl.AbsProjectServiceImpl
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Suppress("ALL", "UNUSED")
@Service
class SimpleProjectServiceImpl @Autowired constructor(
    projectPermissionService: ProjectPermissionService,
    dslContext: DSLContext,
    projectDao: ProjectDao,
    projectJmxApi: ProjectJmxApi,
    redisOperation: RedisOperation,
    client: Client,
    projectDispatcher: ProjectDispatcher,
    authPermissionApi: AuthPermissionApi,
    projectAuthServiceCode: ProjectAuthServiceCode,
    shardingRoutingRuleAssignService: ShardingRoutingRuleAssignService,
    objectMapper: ObjectMapper,
    projectExtService: ProjectExtService,
    projectApprovalService: ProjectApprovalService
) : AbsProjectServiceImpl(
    projectPermissionService = projectPermissionService,
    dslContext = dslContext,
    projectDao = projectDao,
    projectJmxApi = projectJmxApi,
    redisOperation = redisOperation,
    client = client,
    projectDispatcher = projectDispatcher,
    authPermissionApi = authPermissionApi,
    projectAuthServiceCode = projectAuthServiceCode,
    shardingRoutingRuleAssignService = shardingRoutingRuleAssignService,
    objectMapper = objectMapper,
    projectExtService = projectExtService,
    projectApprovalService = projectApprovalService
) {

    override fun getDeptInfo(userId: String): UserDeptDetail {
        return UserDeptDetail(
            bgName = "",
            bgId = "1",
            centerName = "",
            centerId = "1",
            deptName = "",
            deptId = "1",
            groupId = "0",
            groupName = ""
        )
    }

    override fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String {
        // 保存Logo文件
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        val result =
            CommonUtils.serviceUploadFile(
                userId = userId,
                serviceUrlPrefix = serviceUrlPrefix,
                file = logoFile,
                fileChannelType = FileChannelTypeEnum.WEB_SHOW.name,
                language = I18nUtil.getLanguage(userId),
                logo = true
            )
        if (result.isNotOk()) {
            throw OperationException("${result.status}:${result.message}")
        }
        return result.data!!
    }

    override fun deleteAuth(projectId: String, accessToken: String?) {
        projectPermissionService.deleteResource(projectId)
    }

    override fun getProjectFromAuth(userId: String?, accessToken: String?): List<String> {
        return projectPermissionService.getUserProjects(userId!!)
    }

    override fun getProjectFromAuth(
        userId: String,
        accessToken: String?,
        permission: AuthPermission
    ): List<String>? {
        return projectPermissionService.filterProjects(
            userId = userId,
            permission = permission
        )
    }

    override fun isShowUserManageIcon(routerTag: String?): Boolean {
        return projectPermissionService.isShowUserManageIcon()
    }

    override fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo) {
        return
    }

    override fun organizationMarkUp(
        projectCreateInfo: ProjectCreateInfo,
        userDeptDetail: UserDeptDetail
    ): ProjectCreateInfo {
        return projectCreateInfo
    }

    override fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean {
        val validate = projectPermissionService.verifyUserProjectPermission(
            projectCode = projectCode,
            userId = userId,
            permission = permission
        )
        if (!validate) {
            logger.warn("$projectCode| $userId| ${permission.value} validatePermission fail")
            throw PermissionForbiddenException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.PEM_CHECK_FAIL,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        return true
    }

    override fun modifyProjectAuthResource(
        resourceUpdateInfo: ResourceUpdateInfo
    ) {
        projectPermissionService.modifyResource(
            resourceUpdateInfo = resourceUpdateInfo
        )
    }

    override fun cancelCreateAuthProject(userId: String, projectCode: String) {
        projectPermissionService.cancelCreateAuthProject(userId = userId, projectCode = projectCode)
    }

    override fun cancelUpdateAuthProject(userId: String, projectCode: String) {
        projectPermissionService.cancelUpdateAuthProject(userId = userId, projectCode = projectCode)
    }

    override fun createProjectUser(projectId: String, createInfo: ProjectCreateUserInfo): Boolean {
        return true
    }

    override fun isRbacPermission(projectId: String): Boolean = true

    override fun buildRouterTag(routerTag: String?): String? = null

    override fun updateProjectRouterTag(englishName: String) = Unit

    companion object {
        private val logger = LoggerFactory.getLogger(SimpleProjectServiceImpl::class.java)
    }
}
