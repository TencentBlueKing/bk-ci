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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.ProjectVO
import org.slf4j.LoggerFactory

@SuppressWarnings("LongParameterList", "TooManyFunctions")
class RbacPermissionResourceService(
    private val client: Client,
    private val iamV2ManagerService: V2ManagerService,
    private val authResourceService: AuthResourceService,
    private val authResourceGroupService: AuthResourceGroupService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val permissionSubsetManagerService: PermissionSubsetManagerService
) : PermissionResourceService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceService::class.java)
    }

    @SuppressWarnings("LongMethod")
    override fun resourceCreateRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        logger.info("resource create relation|$userId|$projectCode|$resourceType|$resourceCode|$resourceName")
        val managerId = if (resourceType == AuthResourceType.PROJECT.value) {
            permissionGradeManagerService.createGradeManager(
                userId = userId,
                projectCode = projectCode,
                projectName = resourceName,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = resourceCode,
                resourceName = resourceName
            )
        } else {
            // 获取项目管理的权限资源
            val projectInfo = getProjectInfo(projectCode = projectCode)
            permissionSubsetManagerService.createSubsetManager(
                gradeManagerId = projectInfo.relationId!!,
                userId = userId,
                projectCode = projectCode,
                projectName = projectInfo.projectName,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName
            )
        }
        // 项目创建需要审批时,不需要保存资源信息
        if (managerId != 0) {
            authResourceService.create(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                // 项目默认开启权限管理
                enable = resourceType == AuthResourceType.PROJECT.value,
                relationId = managerId.toString()
            )
        }
        return true
    }

    override fun resourceModifyRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean {
        logger.info("resource modify relation|$projectCode|$resourceType|$resourceCode|$resourceName")
        val resourceInfo = authResourceService.get(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val updateAuthResource = if (resourceType == AuthResourceType.PROJECT.value) {
            permissionGradeManagerService.modifyGradeManager(
                gradeManagerId = resourceInfo.relationId,
                projectCode = projectCode,
                projectName = resourceName,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = resourceCode,
                resourceName = resourceName
            )
        } else {
            permissionSubsetManagerService.modifySubsetManager(
                subsetManagerId = resourceInfo.relationId,
                projectCode = projectCode,
                projectName = resourceInfo.resourceName,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName
            )
        }
        if (updateAuthResource) {
            authResourceService.update(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
            )
        }
        return true
    }

    override fun resourceDeleteRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("resource delete relation|$projectCode|$resourceType|$resourceCode")
        if (resourceType == AuthResourceType.PROJECT.value) {
            val projectInfo = getProjectInfo(projectCode)
            permissionGradeManagerService.deleteGradeManager(projectInfo.relationId!!)
        } else {
            val resourceInfo = authResourceService.get(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
            permissionSubsetManagerService.deleteSubsetManager(resourceInfo.relationId)
        }
        authResourceService.delete(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        return true
    }

    override fun resourceCancelRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("resource cancel relation|$projectCode|$resourceType|$resourceCode")
        // 只有项目创建时才可以取消
        if (resourceType == AuthResourceType.PROJECT.value) {
            permissionGradeManagerService.cancelCreateGradeManagerByEnglishName(projectCode)
        }
        return true
    }

    @Suppress("ReturnCount")
    override fun hasManagerPermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        // 1. 先判断是否是项目管理员
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = projectId
        )
        val gradeManagerDetail = iamV2ManagerService.getGradeManagerDetail(projectInfo.relationId)
        if (gradeManagerDetail.members.contains(userId)) {
            return true
        }
        if (resourceType != AuthResourceType.PROJECT.value) {
            // 2. 判断是否是资源管理员
            val resourceInfo = authResourceService.get(
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
            val subsetManagerDetail = iamV2ManagerService.getSubsetManagerDetail(resourceInfo.relationId)
            if (subsetManagerDetail.members.contains(userId)) {
                return true
            }
        }
        return false
    }

    override fun isEnablePermission(
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        return authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        ).enable
    }

    override fun enableResourcePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("enable resource permission|$userId|$projectId|$resourceType|$resourceCode")
        // 1. 先判断是否是项目管理员
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val gradeManagerDetail = iamV2ManagerService.getGradeManagerDetail(projectInfo.relationId)
        if (!gradeManagerDetail.members.contains(userId)) {
            throw PermissionForbiddenException(
                message = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
            )
        }
        // 2. 判断是否是资源管理员
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val subsetManagerDetail = iamV2ManagerService.getSubsetManagerDetail(resourceInfo.relationId)
        if (!subsetManagerDetail.members.contains(userId)) {
            throw PermissionForbiddenException(
                message = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
            )
        }
        // 已经启用的不需要再启用
        if (resourceInfo.enable) {
            logger.info("resource has enable permission manager|$userId|$projectId|$resourceType|$resourceCode")
            return true
        }
        authResourceGroupService.createSubsetManagerDefaultGroup(
            subsetManagerId = resourceInfo.relationId.toInt(),
            userId = userId,
            projectCode = projectId,
            projectName = projectInfo.resourceName,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceInfo.resourceName,
            createMode = true
        )
        return authResourceService.enable(
            userId = userId,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }

    override fun disableResourcePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("disable resource permission|$userId|$projectId|$resourceType|$resourceCode")
        val hasManagerPermission = hasManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            resourceCode
        )
        if (!hasManagerPermission) {
            throw PermissionForbiddenException(
                message = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
            )
        }
        return authResourceService.disable(
            userId = userId,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }

    override fun listResoureces(
        userId: String,
        projectId: String?,
        resourceType: String?,
        resourceName: String?,
        page: Int,
        pageSize: Int
    ): Pagination<AuthResourceInfo> {
        val resourceList = authResourceService.list(
            projectCode = projectId,
            resourceType = resourceType,
            resourceName = resourceName,
            page = page,
            pageSize = pageSize
        )
        if (resourceList.isEmpty()) {
            return Pagination(false, emptyList())
        }
        return Pagination(
            hasNext = resourceList.size == pageSize,
            records = resourceList
        )
    }

    private fun getProjectInfo(projectCode: String): ProjectVO {
        val projectInfo =
            client.get(ServiceProjectResource::class).get(englishName = projectCode).data ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = arrayOf(projectCode),
                defaultMessage = "项目[$projectCode]不存在"
            )
        projectInfo.relationId ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
            params = arrayOf(projectCode),
            defaultMessage = "the resource not exists, projectCode:$projectCode"
        )
        return projectInfo
    }
}
