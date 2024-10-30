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

package com.tencent.devops.auth.provider.rbac.service.migrate

import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.dto.PermissionHandoverDTO
import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupAndMemberFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import org.jboss.logging.Logger
import org.jooq.DSLContext

class MigratePermissionHandoverService(
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceService: AuthResourceService,
    private val resourceGroupAndMemberFacadeService: PermissionResourceGroupAndMemberFacadeService,
    private val dslContext: DSLContext
) {
    fun handoverPermissions(permissionHandoverDTO: PermissionHandoverDTO) {
        val handoverFrom = permissionHandoverDTO.handoverFrom
        val handoverToList = permissionHandoverDTO.handoverToList
        val resourceType = permissionHandoverDTO.resourceType!!
        permissionHandoverDTO.projectList.forEach { projectCode ->
            if (permissionHandoverDTO.managerPermission) {
                batchAddProjectManager(
                    projectCode = projectCode,
                    handoverToList = handoverToList
                )
            }
            val resourceList = authResourceService.listByCreator(
                resourceType = resourceType,
                projectCode = projectCode,
                creator = handoverFrom
            )
            resourceList.forEach { resource ->
                val resourceCode = resource.resourceCode
                val handoverTo = handoverToList.random()
                logger.info("handover resource permissions :$projectCode|$resourceCode|$handoverFrom|$handoverTo")
                authResourceService.updateCreator(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    creator = handoverTo
                )
                val resourceManagerGroup = authResourceGroupDao.get(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    groupCode = DefaultGroupType.MANAGER.value
                )
                try {
                    permissionResourceMemberService.addGroupMember(
                        projectCode = projectCode,
                        memberId = handoverTo,
                        memberType = USER_TYPE,
                        expiredAt = GROUP_EXPIRED_TIME,
                        iamGroupId = resourceManagerGroup!!.relationId.toInt()
                    )
                    permissionResourceMemberService.batchDeleteResourceGroupMembers(
                        projectCode = projectCode,
                        iamGroupId = resourceManagerGroup.relationId.toInt(),
                        members = listOf(handoverFrom)
                    )
                } catch (ignore: Exception) {
                    logger.warn(
                        "handover permissions|operate group failed:$projectCode|" +
                            "${resourceManagerGroup!!.relationId}|${ignore.message}"
                    )
                }
            }
        }
    }

    fun handoverAllPermissions(permissionHandoverDTO: PermissionHandoverDTO) {
        val handoverFrom = permissionHandoverDTO.handoverFrom
        val handoverToList = permissionHandoverDTO.handoverToList
        permissionHandoverDTO.projectList.forEach { projectCode ->
            // 是否将交接人直接加入管理员组
            if (permissionHandoverDTO.managerPermission) {
                batchAddProjectManager(
                    projectCode = projectCode,
                    handoverToList = handoverToList
                )
            }
            // 交接用户组权限
            val userJoinedGroups = resourceGroupAndMemberFacadeService.getMemberGroupsDetails(
                projectId = projectCode,
                memberId = handoverFrom
            ).records.filter { it.joinedType == JoinedType.DIRECT }.map { it.groupId }
            userJoinedGroups.forEach { iamGroupId ->
                val handoverTo = handoverToList.random()
                logger.info("handover resource permissions :$projectCode|$handoverFrom|$handoverTo|$iamGroupId")
                try {
                    permissionResourceMemberService.addGroupMember(
                        projectCode = projectCode,
                        memberId = handoverTo,
                        memberType = USER_TYPE,
                        expiredAt = GROUP_EXPIRED_TIME,
                        iamGroupId = iamGroupId
                    )
                    permissionResourceMemberService.batchDeleteResourceGroupMembers(
                        projectCode = projectCode,
                        iamGroupId = iamGroupId,
                        members = listOf(handoverFrom)
                    )
                } catch (ignore: Exception) {
                    logger.warn(
                        "handover permissions|operate group failed:$projectCode|$iamGroupId|${ignore.message}"
                    )
                }
            }
        }
    }

    private fun batchAddProjectManager(
        projectCode: String,
        handoverToList: List<String>
    ) {
        val projectManagerGroupId = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            groupCode = DefaultGroupType.MANAGER.value
        )
        handoverToList.forEach { handoverTo ->
            permissionResourceMemberService.addGroupMember(
                projectCode = projectCode,
                memberId = handoverTo,
                memberType = USER_TYPE,
                expiredAt = GROUP_EXPIRED_TIME,
                iamGroupId = projectManagerGroupId!!.relationId.toInt()
            )
        }
    }

    companion object {
        // 过期时间为永久
        private const val GROUP_EXPIRED_TIME = 4102444800L
        private const val USER_TYPE = "user"
        private val logger = Logger.getLogger(MigratePermissionHandoverService::class.java)
    }
}
