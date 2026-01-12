/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthAuthorizationDao
import com.tencent.devops.auth.pojo.dto.PermissionBatchValidateDTO
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import jakarta.ws.rs.NotFoundException

class RbacPermissionResourceValidateService(
    private val permissionService: PermissionService,
    private val rbacCommonService: RbacCommonService,
    private val client: Client,
    private val authAuthorizationDao: AuthAuthorizationDao,
    private val dslContext: DSLContext
) : PermissionResourceValidateService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceValidateService::class.java)
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        permissionBatchValidateDTO: PermissionBatchValidateDTO
    ): Map<String, Boolean> {
        logger.info("batch validate user resource permission|$userId|$projectCode|$permissionBatchValidateDTO")
        val watcher = Watcher("batchValidateUserResourcePermission|$userId|$projectCode")
        try {
            val projectActionList = mutableSetOf<String>()
            val resourceActionList = mutableSetOf<String>()

            permissionBatchValidateDTO.actionList.forEach { action ->
                val actionInfo = rbacCommonService.getActionInfo(action)
                val iamRelatedResourceType = actionInfo.relatedResourceType
                if (iamRelatedResourceType == AuthResourceType.PROJECT.value) {
                    projectActionList.add(action)
                } else {
                    resourceActionList.add(action)
                }
            }

            val actionCheckPermissionMap = mutableMapOf<String, Boolean>()
            // 验证项目下的权限
            if (projectActionList.isNotEmpty()) {
                watcher.start("batchValidateProjectAction")
                actionCheckPermissionMap.putAll(
                    validateProjectPermission(
                        userId = userId,
                        actions = projectActionList.toList(),
                        projectCode = projectCode
                    )
                )
            }
            // 验证具体资源权限
            if (resourceActionList.isNotEmpty()) {
                watcher.start("batchValidateResourceAction")
                actionCheckPermissionMap.putAll(
                    validateResourcePermission(
                        userId = userId,
                        projectCode = projectCode,
                        actions = resourceActionList.toList(),
                        resourceType = permissionBatchValidateDTO.resourceType,
                        resourceCode = permissionBatchValidateDTO.resourceCode
                    )
                )
            }
            return actionCheckPermissionMap
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    override fun hasManagerPermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean {
        checkProjectApprovalStatus(resourceType, resourceCode)
        val checkProjectManage = permissionService.checkProjectManager(
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

    override fun validateUserProjectPermissionByChannel(
        userId: String,
        projectCode: String,
        operateChannel: OperateChannel,
        targetMemberId: String
    ) {
        if (operateChannel == OperateChannel.PERSONAL) {
            // 个人视角校验
            val hasVisitPermission = permissionService.validateUserResourcePermission(
                userId = userId,
                resourceType = AuthResourceType.PROJECT.value,
                action = RbacAuthUtils.buildAction(AuthPermission.VISIT, AuthResourceType.PROJECT),
                projectCode = projectCode
            )
            if (hasVisitPermission) return

            val isUserHasProjectAuthorizations = authAuthorizationDao.count(
                dslContext = dslContext,
                condition = ResourceAuthorizationConditionRequest(
                    projectCode = projectCode,
                    handoverFrom = userId
                )
            ) > 0
            if (!isUserHasProjectAuthorizations) {
                throw PermissionForbiddenException(
                    message = "The user does not have permission to visit the project!"
                )
            }
            if (userId != targetMemberId) {
                throw PermissionForbiddenException(
                    message = "You do not have permission to operate other user groups!"
                )
            }
        } else {
            // 管理员视角校验
            val hasProjectManagePermission = permissionService.validateUserResourcePermission(
                userId = userId,
                resourceType = AuthResourceType.PROJECT.value,
                action = RbacAuthUtils.buildAction(AuthPermission.MANAGE, AuthResourceType.PROJECT),
                projectCode = projectCode
            )
            if (!hasProjectManagePermission) {
                throw PermissionForbiddenException(
                    message = I18nUtil.getCodeLanMessage(AuthMessageCode.ERROR_AUTH_NO_MANAGE_PERMISSION)
                )
            }
        }
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

    private fun validateProjectPermission(
        userId: String,
        actions: List<String>,
        projectCode: String
    ): Map<String, Boolean> {
        return permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resourceCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value
        )
    }

    private fun validateResourcePermission(
        userId: String,
        projectCode: String,
        actions: List<String>,
        resourceType: String,
        resourceCode: String
    ): Map<String, Boolean> {
        return permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }
}
