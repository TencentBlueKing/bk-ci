/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.code.BSExperienceAuthServiceCode
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
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response

@Service
class GroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val groupDao: GroupDao,
    private val objectMapper: ObjectMapper,
    private val bsAuthPermissionApi: BSAuthPermissionApi,
    private val bsAuthResourceApi: BSAuthResourceApi,
    private val bsAuthProjectApi: BSAuthProjectApi,
    private val experienceServiceCode: BSExperienceAuthServiceCode
) {

    private val resourceType = AuthResourceType.EXPERIENCE_GROUP
    private val regex = Pattern.compile(",|;")

    fun list(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<GroupSummaryWithPermission>> {
        val groupPermissionListMap = filterGroup(userId, projectId, setOf(AuthPermission.EDIT, AuthPermission.DELETE))

        val count = groupDao.count(dslContext, projectId)
        val finalLimit = if (limit == -1) count.toInt() else limit
        val list = groupDao.list(dslContext, projectId, offset, finalLimit).map {
            val canEdit = groupPermissionListMap[AuthPermission.EDIT]!!.contains(it.id)
            val canDelete = groupPermissionListMap[AuthPermission.DELETE]!!.contains(it.id)
            GroupSummaryWithPermission(
                    HashUtil.encodeLongId(it.id),
                    it.name,
                    it.innerUsersCount,
                    it.outerUsersCount,
                    objectMapper.readValue(it.innerUsers),
                    it.outerUsers,
                    it.creator,
                    it.remark ?: "",
                    GroupPermission(canEdit, canDelete)
            )
        }
        return Pair(count, list)
    }

    fun getProjectUsers(userId: String, projectId: String, projectGroup: ProjectGroup?): List<String> {
        val bkAuthGroup = if (projectGroup == null) null else BkAuthGroup.valueOf(projectGroup.name)
        return bsAuthProjectApi.getProjectUsers(experienceServiceCode, projectId, bkAuthGroup)
    }

    fun getProjectGroupAndUsers(userId: String, projectId: String): List<ProjectGroupAndUsers> {
        val groupAndUsersList = bsAuthProjectApi.getProjectGroupAndUserList(experienceServiceCode, projectId)
        return groupAndUsersList.map {
            ProjectGroupAndUsers(
                    it.displayName,
                    it.roleName,
                    it.roleId,
                    it.userIdList.toSet()
            )
        }
    }

    fun serviceCheck(groupHashId: String): Boolean {
        return groupDao.getOrNull(dslContext, HashUtil.decodeIdToLong(groupHashId)) != null
    }

    fun create(projectId: String, userId: String, group: GroupCreate) {
        if (groupDao.has(dslContext, projectId, group.name)) {
            throw CustomException(Response.Status.BAD_REQUEST, "体验组(${group.name})已存在")
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        val groupId = groupDao.create(dslContext,
                projectId,
                group.name,
                objectMapper.writeValueAsString(group.innerUsers),
                innerUsersCount,
                group.outerUsers,
                outerUsersCount,
                group.remark,
                userId,
                userId)
        createResource(userId, projectId, groupId, group.name)
    }

    fun get(userId: String, projectId: String, groupHashId: String): Group {
        return serviceGet(groupHashId)
    }

    fun serviceGet(groupHashId: String): Group {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val groupRecord = groupDao.get(dslContext, groupId)
        return Group(
                groupHashId,
                groupRecord.name,
                objectMapper.readValue(groupRecord.innerUsers),
                groupRecord.outerUsers,
                groupRecord.remark ?: ""
        )
    }

    fun serviceGet(groupHashIdSet: Set<String>): Map<String, Group> {
        val groupIds = groupHashIdSet.map { HashUtil.decodeIdToLong(it) }.toSet()
        val map = mutableMapOf<String, Group>()
        groupDao.list(dslContext, groupIds).forEach {
            val groupHashId = HashUtil.encodeLongId(it.id)
            map[groupHashId] = Group(
                    groupHashId,
                    it.name,
                    objectMapper.readValue(it.innerUsers),
                    it.outerUsers,
                    it.remark ?: ""
            )
        }
        return map
    }

    fun serviceList(groupHashIds: Set<String>): List<Group> {
        val groupIds = groupHashIds.map { HashUtil.decodeIdToLong(it) }.toSet()
        val groupRecords = groupDao.list(dslContext, groupIds)
        return groupRecords.map {
            Group(
                    HashUtil.encodeLongId(it.id),
                    it.name,
                    objectMapper.readValue(it.innerUsers),
                    it.outerUsers,
                    it.remark ?: ""
            )
        }
    }

    fun getUsers(userId: String, projectId: String, groupHashId: String): GroupUsers {
        return serviceGetUsers(groupHashId)
    }

    fun serviceGetUsers(groupHashId: String): GroupUsers {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val groupRecord = groupDao.get(dslContext, groupId)
        val innerUsers = objectMapper.readValue<Set<String>>(groupRecord.innerUsers)
        val outerUsers = regex.split(groupRecord.outerUsers).toSet()
        return GroupUsers(innerUsers, outerUsers)
    }

    fun edit(userId: String, projectId: String, groupHashId: String, group: GroupUpdate) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        validatePermission(userId, projectId, groupId, AuthPermission.EDIT, "用户在项目($projectId)没有体验组($groupHashId)的编辑权限")
        if (groupDao.getOrNull(dslContext, groupId) == null) {
            throw NotFoundException("体验组($groupHashId)不存在")
        }
        if (groupDao.has(dslContext, projectId, group.name, groupId)) {
            throw CustomException(Response.Status.BAD_REQUEST, "体验组(${group.name})已存在")
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        groupDao.update(dslContext,
                groupId,
                group.name,
                objectMapper.writeValueAsString(group.innerUsers),
                innerUsersCount,
                group.outerUsers,
                outerUsersCount,
                group.remark,
                userId)
        modifyResource(projectId, groupId, group.name)
    }

    fun delete(userId: String, projectId: String, groupHashId: String) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        validatePermission(userId, projectId, groupId, AuthPermission.DELETE, "用户在项目($projectId)没有体验组($groupHashId)的删除权限")

        deleteResource(projectId, groupId)
        groupDao.delete(dslContext, groupId)
    }

    private fun validatePermission(userId: String, projectId: String, groupId: Long, authPermission: AuthPermission, message: String) {
        if (!bsAuthPermissionApi.validateUserResourcePermission(userId, experienceServiceCode, resourceType, projectId, HashUtil.encodeLongId(groupId), authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    private fun createResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        bsAuthResourceApi.createResource(userId, experienceServiceCode, resourceType, projectId, HashUtil.encodeLongId(groupId), groupName)
    }

    private fun modifyResource(projectId: String, groupId: Long, groupName: String) {
        bsAuthResourceApi.modifyResource(experienceServiceCode, resourceType, projectId, HashUtil.encodeLongId(groupId), groupName)
    }

    private fun deleteResource(projectId: String, groupId: Long) {
        bsAuthResourceApi.deleteResource(experienceServiceCode, resourceType, projectId, HashUtil.encodeLongId(groupId))
    }

    private fun filterGroup(user: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bsAuthPermissionApi.getUserResourcesByPermissions(user, experienceServiceCode, resourceType, projectId, authPermissions, null)
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { key, value ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }
        return map
    }
}