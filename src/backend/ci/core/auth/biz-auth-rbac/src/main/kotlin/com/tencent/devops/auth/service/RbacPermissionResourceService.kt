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
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_RESOURCE_CREATE_FAIL
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.enums.AuthGroupCreateMode
import com.tencent.devops.auth.pojo.event.AuthResourceGroupCreateEvent
import com.tencent.devops.auth.pojo.event.AuthResourceGroupModifyEvent
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import org.slf4j.LoggerFactory
import javax.ws.rs.NotFoundException

@SuppressWarnings("LongParameterList", "TooManyFunctions")
class RbacPermissionResourceService(
    private val authResourceService: AuthResourceService,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    private val permissionSubsetManagerService: PermissionSubsetManagerService,
    private val authResourceCodeConverter: AuthResourceCodeConverter,
    private val permissionService: PermissionService,
    private val permissionProjectService: PermissionProjectService,
    private val traceEventDispatcher: TraceEventDispatcher,
    private val iamV2ManagerService: V2ManagerService,
    private val client: Client
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
        resourceName: String,
        async: Boolean
    ): Boolean {
        logger.info("resource create relation|$userId|$projectCode|$resourceType|$resourceCode|$resourceName")
        val iamResourceCode = authResourceCodeConverter.generateIamCode(
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        var projectName = resourceName
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
            // 获取分级管理员信息
            val projectInfo = authResourceService.get(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            )
            projectName = projectInfo.resourceName
            permissionSubsetManagerService.createSubsetManager(
                gradeManagerId = projectInfo.relationId,
                userId = userId,
                projectCode = projectCode,
                projectName = projectInfo.resourceName,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                iamResourceCode = iamResourceCode
            )
        }
        // 项目创建需要审批时,不需要保存资源信息,审批通过回调后，再进行创建。
        val isCreateResourceAndGroup = managerId != 0
        if (isCreateResourceAndGroup) {
            createResource(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceName = resourceName,
                resourceCode = resourceCode,
                iamResourceCode = iamResourceCode,
                managerId = managerId
            )
            createResourceDefaultGroup(
                userId = userId,
                projectCode = projectCode,
                projectName = projectName,
                resourceType = resourceType,
                resourceName = resourceName,
                resourceCode = resourceCode,
                iamResourceCode = iamResourceCode,
                async = async,
                managerId = managerId
            )
        }
        return true
    }

    private fun createResource(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceName: String,
        resourceCode: String,
        iamResourceCode: String,
        managerId: Int
    ) {
        try {
            authResourceService.create(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                iamResourceCode = iamResourceCode,
                // 流水线组需要主动开启权限管理
                enable = resourceType != AuthResourceType.PIPELINE_GROUP.value,
                relationId = managerId.toString()
            )
        } catch (ignore: Exception) {
            if (resourceType == AuthResourceType.PROJECT.value) {
                iamV2ManagerService.deleteManagerV2(managerId.toString())
            } else {
                iamV2ManagerService.deleteSubsetManager(managerId.toString())
            }
            logger.warn("create resource failed|$userId|$projectCode|$resourceType|$resourceName", ignore)
            throw ErrorCodeException(
                errorCode = ERROR_RESOURCE_CREATE_FAIL,
                defaultMessage = "create resource failed|$userId|$projectCode|$resourceType|$resourceName"
            )
        }
    }

    private fun createResourceDefaultGroup(
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceName: String,
        resourceCode: String,
        iamResourceCode: String,
        async: Boolean,
        managerId: Int
    ) {
        if (async) {
            traceEventDispatcher.dispatch(
                AuthResourceGroupCreateEvent(
                    managerId = managerId,
                    userId = userId,
                    projectCode = projectCode,
                    projectName = projectName,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    resourceName = resourceName,
                    iamResourceCode = iamResourceCode
                )
            )
        } else {
            // 同步创建组，主要用于迁移数据；正常创建资源，走异步
            if (resourceType == AuthResourceType.PROJECT.value) {
                permissionGradeManagerService.createGradeDefaultGroup(
                    gradeManagerId = managerId,
                    userId = userId,
                    projectCode = projectCode,
                    projectName = projectName
                )
            } else {
                permissionSubsetManagerService.createSubsetManagerDefaultGroup(
                    subsetManagerId = managerId,
                    userId = userId,
                    projectCode = projectCode,
                    projectName = projectName,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    resourceName = resourceName,
                    iamResourceCode = iamResourceCode,
                    createMode = AuthGroupCreateMode.CREATE
                )
            }
        }
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
                projectName = resourceName
            )
        } else {
            permissionSubsetManagerService.modifySubsetManager(
                subsetManagerId = resourceInfo.relationId,
                projectCode = projectCode,
                projectName = resourceInfo.resourceName,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                iamResourceCode = resourceInfo.iamResourceCode
            )
        }
        if (updateAuthResource) {
            authResourceService.update(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName
            )
            traceEventDispatcher.dispatch(
                AuthResourceGroupModifyEvent(
                    managerId = resourceInfo.relationId.toInt(),
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    resourceName = resourceName
                )
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
        // 项目不能删除,不需要删除分级管理员
        if (resourceType != AuthResourceType.PROJECT.value) {
            val resourceInfo = authResourceService.getOrNull(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
            if (resourceInfo != null) {
                permissionSubsetManagerService.deleteSubsetManager(resourceInfo.relationId)
            }
        }
        authResourceService.delete(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        return true
    }

    override fun resourceCancelRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        logger.info("resource cancel relation|$projectCode|$resourceType|$resourceCode")
        // 只有项目才可以取消
        if (resourceType == AuthResourceType.PROJECT.value) {
            permissionGradeManagerService.userCancelApplication(
                userId = userId,
                projectCode = projectCode
            )
        }
        return true
    }

    override fun hasManagerPermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        checkProjectApprovalStatus(resourceType, resourceCode)
        val checkProjectManage = permissionProjectService.checkProjectManager(
            userId = userId,
            projectCode = projectId
        )
        if (checkProjectManage) {
            return true
        }

        // TODO 流水线组一期先不上,流水线组权限由项目控制
        if (resourceType == AuthResourceType.PROJECT.value || resourceType == AuthResourceType.PIPELINE_GROUP.value) {
            throw PermissionForbiddenException(
                message = I18nUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
            )
        } else {
            val checkResourceManage = permissionService.validateUserResourcePermissionByRelation(
                userId = userId,
                action = RbacAuthUtils.buildAction(
                    authPermission = AuthPermission.MANAGE,
                    authResourceType = RbacAuthUtils.getResourceTypeByStr(resourceType)
                ),
                projectCode = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                relationResourceType = null
            )
            if (!checkResourceManage) {
                throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
                )
            }
        }
        return true
    }

    private fun checkProjectApprovalStatus(resourceType: String, resourceCode: String) {
        if (resourceType == AuthResourceType.PROJECT.value) {
            val projectInfo =
                client.get(ServiceProjectResource::class).get(resourceCode).data
                    ?: throw NotFoundException("project - $resourceCode is not exist!")
            val approvalStatus = ProjectApproveStatus.parse(projectInfo.approvalStatus)
            if (approvalStatus.isCreatePending()) {
                throw ErrorCodeException(
                    errorCode = ProjectMessageCode.UNDER_APPROVAL_PROJECT,
                    params = arrayOf(resourceCode),
                    defaultMessage = "project $resourceCode is being approved, " +
                        "please wait patiently, or contact the approver"
                )
            }
        }
    }

    override fun isEnablePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        // 项目不能进入[成员管理]页面,其他资源不需要校验权限，有普通成员视角查看权限页面
        if (resourceType == AuthResourceType.PROJECT.value) {
            hasManagerPermission(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        }
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
        hasManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        // 已经启用的不需要再启用
        if (resourceInfo.enable) {
            logger.info("resource has enable permission manager|$userId|$projectId|$resourceType|$resourceCode")
            return true
        }
        permissionSubsetManagerService.createSubsetManagerDefaultGroup(
            subsetManagerId = resourceInfo.relationId.toInt(),
            userId = userId,
            projectCode = projectId,
            projectName = projectInfo.resourceName,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceInfo.resourceName,
            iamResourceCode = resourceInfo.iamResourceCode,
            createMode = AuthGroupCreateMode.ENABLE
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
        hasManagerPermission(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            resourceCode
        )
        if (resourceType == AuthResourceType.PROJECT.value) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_PROJECT_PERMISSION_CLOSE_FAIL,
                defaultMessage = "project permission management cannot be turned off"
            )
        }
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        // 已禁用的项目不需要再禁用
        if (!resourceInfo.enable) {
            logger.info("resource has enable permission manager|$userId|$projectId|$resourceType|$resourceCode")
            return true
        }
        permissionSubsetManagerService.deleteSubsetManagerDefaultGroup(
            subsetManagerId = resourceInfo.relationId.toInt(),
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        authResourceService.disable(
            userId = userId,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
        return true
    }

    override fun listResources(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceName: String?,
        page: Int,
        pageSize: Int
    ): Pagination<AuthResourceInfo> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val resourceList = authResourceService.list(
            projectCode = projectId,
            resourceType = resourceType,
            resourceName = resourceName,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        )
        if (resourceList.isEmpty()) {
            return Pagination(false, emptyList())
        }
        return Pagination(
            hasNext = resourceList.size == pageSize,
            records = resourceList
        )
    }

    override fun getResource(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): AuthResourceInfo {
        return authResourceService.get(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }
}
