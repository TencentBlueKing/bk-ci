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

package com.tencent.devops.auth.service.ci.impl

import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.auth.service.AuthCustomizePermissionService
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.action.ActionService
import com.tencent.devops.auth.service.action.BkResourceService
import com.tencent.devops.auth.service.ci.PermissionRoleService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbsPermissionRoleServiceImpl @Autowired constructor(
    private val groupService: AuthGroupService,
    private val resourceService: BkResourceService,
    private val actionsService: ActionService
) : PermissionRoleService {
    override fun createPermissionRole(
        userId: String,
        projectId: String,
        projectCode: String,
        groupInfo: ProjectRoleDTO
    ): Int {
        var groupType = groupInfo.defaultGroup
        var groupName = ""
        var displayName = ""
        if (!DefaultGroupType.contains(groupInfo.code)) {
            groupType = false
            groupName = groupInfo.name ?: ""
            displayName = groupInfo.displayName ?: groupInfo.name
        } else {
            groupType = true
            groupName = groupInfo.name
            displayName = DefaultGroupType.get(groupInfo.code).displayName
        }
        val roleId = groupService.createGroup(
            userId = userId,
            projectCode = projectCode,
            groupInfo = GroupDTO(
                groupCode = groupInfo.code,
                groupType = groupType,
                groupName = groupName,
                displayName = displayName,
                relationId = null,
                desc = groupInfo.desc
            )
        )
        try {
            // 扩展系统添加用户组. 可根据自身情况做扩展
            groupCreateExt(
                roleId = roleId,
                userId = userId,
                projectId = projectId,
                projectCode = projectCode,
                groupInfo = groupInfo
            )
            logger.info("create ext group success $projectCode $roleId")
        } catch (iamException: IamException) {
            logger.warn("create Role ext fail $iamException")
            groupService.deleteGroup(roleId, false)
            throw RemoteServiceException("create project role fail: ${iamException.errorMsg}")
        } catch (e: Exception) {
            logger.warn("create Role ext fail $e")
            groupService.deleteGroup(roleId, false)
            throw ParamBlankException("create project role fail")
        }
        return roleId
    }

    override fun updatePermissionRole(
        userId: String,
        projectId: String,
        roleId: Int,
        groupInfo: ProjectRoleDTO
    ) {
        groupService.updateGroup(userId, roleId, groupInfo)
        // 关联系统同步修改
        updateGroupExt(
            userId = userId,
            projectId = projectId,
            roleId = roleId,
            groupInfo = groupInfo
        )
    }

    override fun deletePermissionRole(userId: String, projectId: String, roleId: Int) {
        // 优先删除扩展系统内的数据,最后再删本地数据
        deleteRoleExt(userId, projectId, roleId)
        groupService.deleteGroup(roleId)
    }

    override fun rolePermissionStrategy(
        userId: String,
        projectCode: String,
        roleId: Int,
        permissionStrategy: Map<String, List<String>>
    ): Boolean {
        val groupInfo = groupService.getGroupCode(roleId) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.GROUP_NOT_EXIST,
            defaultMessage = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.GROUP_NOT_EXIST)
        )
        // 默认用户组不能调整权限策略
        if (!groupInfo.groupType) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GROUP_NOT_EXIST,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.DEFAULT_GROUP_NOT_ALLOW_UPDATE)
            )
        }
        permissionStrategy.forEach { resource, actions ->
            // 校验资源和action是否存在
            if (resourceService.getResource(resource) == null) {
                AuthCustomizePermissionService.logger.info("createCustomizePermission $userId$roleId$resource not exist")
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.RESOURCE_NOT_EXSIT,
                    defaultMessage = MessageCodeUtil.getCodeMessage(AuthMessageCode.RESOURCE_NOT_EXSIT, arrayOf(resource))
                )
            }

            if (!actionsService.checkSystemAction(actions)) {
                AuthCustomizePermissionService.logger.info("createCustomizePermission $userId$roleId$actions not exist")
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.PERMISSION_MODEL_CHECK_FAIL,
                    defaultMessage = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.PERMISSION_MODEL_CHECK_FAIL)
                )
            }
        }
        return rolePermissionStrategyExt(userId, projectCode, roleId, permissionStrategy)
    }

    abstract fun groupCreateExt(
        roleId: Int,
        userId: String,
        projectId: String,
        projectCode: String,
        groupInfo: ProjectRoleDTO
    )

    abstract fun updateGroupExt(
        userId: String,
        projectId: String,
        roleId: Int,
        groupInfo: ProjectRoleDTO
    )

    abstract fun deleteRoleExt(
        userId: String,
        projectId: String,
        roleId: Int
    )

    abstract fun rolePermissionStrategyExt(
        userId: String,
        projectCode: String,
        roleId: Int,
        permissionStrategy: Map<String, List<String>>
    ): Boolean

    companion object {
        private val logger = LoggerFactory.getLogger(AbsPermissionRoleServiceImpl::class.java)
    }
}
