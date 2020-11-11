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

package com.tencent.devops.experience.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.ExperienceAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.GroupDao
import com.tencent.devops.experience.pojo.Group
import com.tencent.devops.experience.pojo.GroupCreate
import com.tencent.devops.experience.pojo.GroupPermission
import com.tencent.devops.experience.pojo.GroupSummaryWithPermission
import com.tencent.devops.experience.pojo.GroupUpdate
import com.tencent.devops.experience.pojo.GroupUsers
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.enums.ProjectGroup
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import javax.ws.rs.core.Response

@Service
class GroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val groupDao: GroupDao,
    private val objectMapper: ObjectMapper,
    private val bsAuthPermissionApi: AuthPermissionApi,
    private val bsAuthResourceApi: AuthResourceApi,
    private val bsAuthProjectApi: AuthProjectApi,
    private val experienceServiceCode: ExperienceAuthServiceCode,
    private val experienceGroupInnerDao: ExperienceGroupInnerDao
) {

    private val resourceType = AuthResourceType.EXPERIENCE_GROUP
    private val regex = Pattern.compile(",|;")

    fun list(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<GroupSummaryWithPermission>> {
        val groupPermissionListMap = filterGroup(
            user = userId,
            projectId = projectId,
            authPermissions = setOf(AuthPermission.EDIT, AuthPermission.DELETE)
        )

        val count = groupDao.count(dslContext, projectId)
        val finalLimit = if (limit == -1) count.toInt() else limit
        val groups = groupDao.list(dslContext, projectId, offset, finalLimit)
        val groupIds = groups.map { it.id }.toSet()

        val groupIdToUserIds = getGroupIdToUserIds(groupIds)

        val list = groups.map {
            val canEdit = groupPermissionListMap[AuthPermission.EDIT]!!.contains(it.id)
            val canDelete = groupPermissionListMap[AuthPermission.DELETE]!!.contains(it.id)
            GroupSummaryWithPermission(
                groupHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                innerUsersCount = groupIdToUserIds[it.id]?.size ?: 0,
                outerUsersCount = it.outerUsersCount,
                innerUsers = groupIdToUserIds[it.id] ?: emptySet(),
                outerUsers = it.outerUsers,
                creator = it.creator,
                remark = it.remark ?: "",
                permissions = GroupPermission(canEdit, canDelete)
            )
        }
        return Pair(count, list)
    }

    fun getGroupIdToUserIds(groupIds: Set<Long>): MutableMap<Long, MutableSet<String>> {
        val groupIdToUserIds = mutableMapOf<Long, MutableSet<String>>()
        experienceGroupInnerDao.listByGroupIds(dslContext, groupIds).forEach {
            var userIds = groupIdToUserIds[it.groupId]
            if (null == userIds) {
                userIds = mutableSetOf()
            }
            userIds.add(it.userId)
            groupIdToUserIds[it.groupId] = userIds
        }
        return groupIdToUserIds
    }

    fun getProjectUsers(userId: String, projectId: String, projectGroup: ProjectGroup?): List<String> {
        val bkAuthGroup = if (projectGroup == null) null else BkAuthGroup.valueOf(projectGroup.name)
        return bsAuthProjectApi.getProjectUsers(experienceServiceCode, projectId, bkAuthGroup)
    }

    fun getProjectGroupAndUsers(userId: String, projectId: String): List<ProjectGroupAndUsers> {
        val groupAndUsersList = bsAuthProjectApi.getProjectGroupAndUserList(experienceServiceCode, projectId)
        return groupAndUsersList.map {
            ProjectGroupAndUsers(
                groupName = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${it.roleName}",
                    defaultMessage = it.displayName
                ),
                groupId = it.roleName,
                groupRoleId = it.roleId,
                users = it.userIdList.toSet()
            )
        }
    }

    fun serviceCheck(groupHashId: String): Boolean {
        return groupDao.getOrNull(dslContext, HashUtil.decodeIdToLong(groupHashId)) != null
    }

    fun create(projectId: String, userId: String, group: GroupCreate) {
        if (groupDao.has(dslContext, projectId, group.name)) {
            throw ErrorCodeException(
                defaultMessage = "体验组(${group.name})已存在",
                errorCode = ExperienceMessageCode.EXP_GROUP_IS_EXISTS,
                params = arrayOf(group.name)
            )
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        val groupId = groupDao.create(
            dslContext = dslContext,
            projectId = projectId,
            name = group.name,
            innerUsers = objectMapper.writeValueAsString(group.innerUsers),
            innerUsersCount = innerUsersCount,
            outerUsers = group.outerUsers,
            outerUsersCount = outerUsersCount,
            remark = group.remark,
            creator = userId,
            updator = userId
        )
        createResource(userId = userId, projectId = projectId, groupId = groupId, groupName = group.name)
    }

    fun get(userId: String, projectId: String, groupHashId: String): Group {
        return serviceGet(groupHashId)
    }

    fun serviceGet(groupHashId: String): Group {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val groupRecord = groupDao.get(dslContext, groupId)
        val userIds = experienceGroupInnerDao.listByGroupIds(dslContext, setOf(groupId)).map { it.userId }.toSet()
        return Group(
            groupHashId = groupHashId,
            name = groupRecord.name,
            innerUsers = userIds,
            outerUsers = groupRecord.outerUsers,
            remark = groupRecord.remark ?: ""
        )
    }

    fun serviceGet(groupHashIdSet: Set<String>): Map<String, Group> {
        val groupIds = groupHashIdSet.map { HashUtil.decodeIdToLong(it) }.toSet()

        val groupIdToUserIds = getGroupIdToUserIds(groupIds)
        val map = mutableMapOf<String, Group>()

        groupDao.list(dslContext, groupIds).forEach {
            val groupHashId = HashUtil.encodeLongId(it.id)
            map[groupHashId] = Group(
                groupHashId = groupHashId,
                name = it.name,
                innerUsers = groupIdToUserIds[it.id]?.toSet() ?: emptySet(),
                outerUsers = it.outerUsers,
                remark = it.remark ?: ""
            )
        }
        return map
    }

    fun serviceList(groupHashIds: Set<String>): List<Group> {
        val groupIds = groupHashIds.map { HashUtil.decodeIdToLong(it) }.toSet()
        val groupIdToUserIds = getGroupIdToUserIds(groupIds)

        val groupRecords = groupDao.list(dslContext, groupIds)
        return groupRecords.map {
            Group(
                groupHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                innerUsers = groupIdToUserIds[it.id]?.toSet() ?: emptySet(),
                outerUsers = it.outerUsers,
                remark = it.remark ?: ""
            )
        }
    }

    fun getUsers(userId: String, projectId: String, groupHashId: String): GroupUsers {
        return serviceGetUsers(groupHashId)
    }

    fun serviceGetUsers(groupHashId: String): GroupUsers {
        val groupId = HashUtil.decodeIdToLong(groupHashId)

        val groupRecord = groupDao.get(dslContext, groupId)
        val innerUsers = experienceGroupInnerDao.listByGroupIds(dslContext, setOf(groupId)).map { it.userId }.toSet()
        val outerUsers = regex.split(groupRecord.outerUsers).toSet()
        return GroupUsers(innerUsers, outerUsers)
    }

    fun edit(userId: String, projectId: String, groupHashId: String, group: GroupUpdate) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        validatePermission(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            authPermission = AuthPermission.EDIT,
            message = "用户在项目($projectId)没有体验组($groupHashId)的编辑权限"
        )
        if (groupDao.getOrNull(dslContext, groupId) == null) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                defaultMessage = "体验组($groupHashId)不存在",
                errorCode = ExperienceMessageCode.EXP_GROUP_NOT_EXISTS,
                params = arrayOf(groupHashId)
            )
        }
        if (groupDao.has(dslContext, projectId, group.name, groupId)) {
            throw ErrorCodeException(
                defaultMessage = "体验组(${group.name})已存在",
                errorCode = ExperienceMessageCode.EXP_GROUP_IS_EXISTS,
                params = arrayOf(group.name)
            )
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        groupDao.update(
            dslContext = dslContext,
            id = groupId,
            name = group.name,
            innerUsers = objectMapper.writeValueAsString(group.innerUsers),
            innerUsersCount = innerUsersCount,
            outerUsers = group.outerUsers,
            outerUsersCount = outerUsersCount,
            remark = group.remark,
            updator = userId
        )
        modifyResource(projectId = projectId, groupId = groupId, groupName = group.name)
    }

    fun delete(userId: String, projectId: String, groupHashId: String) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        validatePermission(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            authPermission = AuthPermission.DELETE,
            message = "用户在项目($projectId)没有体验组($groupHashId)的删除权限"
        )

        deleteResource(projectId, groupId)
        groupDao.delete(dslContext, groupId)
    }

    private fun validatePermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!bsAuthPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = experienceServiceCode,
                resourceType = resourceType,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(groupId),
                permission = authPermission
            )
        ) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ExperienceMessageCode.USER_NEED_EXP_GROUP_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permissionMsg)
            )
        }
    }

    private fun createResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        bsAuthResourceApi.createResource(
            user = userId,
            serviceCode = experienceServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    private fun modifyResource(projectId: String, groupId: Long, groupName: String) {
        bsAuthResourceApi.modifyResource(
            serviceCode = experienceServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    private fun deleteResource(projectId: String, groupId: Long) {
        bsAuthResourceApi.deleteResource(
            serviceCode = experienceServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId)
        )
    }

    private fun filterGroup(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bsAuthPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = experienceServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = null
        )
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { (key, value) ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }
        return map
    }
}